package util.common;

import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.StopWatch;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Players;
import util.Activities;
import util.Globals;
import org.rspeer.ui.Log;
import util.Predicates;

import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Activities are composable units of work that are set to automatically have anti-ban measures between tasks.
 * In addition to making some in game action, activities can also specify their lifespans, conditions to run, and
 * relationships to other activities.
 * <p>
 * A single activity is not thread safe, and should only ever be performed once at a time. If multiple threads want
 * to form activities, use a thread-safe queue to queue up activities and then on the main thread have an activity that
 * just pops of activities and runs them.
 */
public class Activity implements Runnable {
    private final List<BooleanSupplier> preConditions;
    private final List<Runnable> activities;
    private final StopWatch stopWatch;
    private int iteration;
    private Optional<String> name;
    private Optional<Activity> parent;
    private long pauseMillis;
    private Optional<ActivityConfigModel> configModel;

    // private because you should only create activities through Activity.newBuilder()...build().
    private Activity(List<BooleanSupplier> preConditions,
                     List<Runnable> activities,
                     Optional<String> name,
                     long pauseMillis,
                     Optional<ActivityConfigModel> configModel) {
        // Pass down context information to any child activities
        activities = activities.stream()
                .map(runnable -> {
                    if (!Activity.class.isInstance(runnable)) {
                        return runnable;
                    }

                    Activity activity = Activity.class.cast(runnable);
                    activity.setParent(this);
                    return activity;
                })
                .collect(Collectors.toList());

        this.preConditions = preConditions;
        this.activities = activities;
        this.stopWatch = StopWatch.start();
        this.iteration = 1;
        this.name = name;
        this.parent = Optional.empty();
        this.pauseMillis = pauseMillis;
        this.configModel = configModel;
    }

    private Activity setParent(Activity parent) {
        this.parent = Optional.of(parent);
        return this;
    }

    /**
     * Finds the nearest parent that has a config model, returning it.
     */
    public Optional<ActivityConfigModel> getConfigModel() {
        if (configModel.isPresent()) return configModel;
        return parent.flatMap(Activity::getConfigModel);
    }

    /**
     * Returns the full name of this activity, which is the name of the current activity along with all parent activity
     * names, separated by a space.
     */
    private Optional<String> getFullName() {
        return this.name.map(name -> this.parent.flatMap(Activity::getFullName).map(s -> s + " : ").orElse("") + name);
    }

    /**
     * Creates an activity that just performs a runnable
     */
    public static Activity of(Runnable runnable) {
        return Activity.newBuilder().addSubActivity(runnable).build();
    }

    /**
     * Creates an activity with default values set. Ideally, activities should be immutable and this should create
     * an Activity.Builder. Laziness prevents me from doing so.
     */
    public static Activity.Builder newBuilder() {
        return new Activity.Builder();
    }

    /**
     * Performs the activity.  By making an Activity a runnable, plain lambdas may be passed in as subactivities.
     */
    @Override
    public void run() {
        stopWatch.reset(); // Reset so duration is just for a current run.
        iteration = 1;

        while (preConditions.stream().allMatch(BooleanSupplier::getAsBoolean)) {
            if (getFullName().isPresent() && iteration == 1) {
                Log.info("Current Activity : " + getFullName().get());
            }

            for (int i = 0; i < activities.size(); ++i) {
                Runnable runnable = activities.get(i);
                runnable.run();

                if (i == activities.size() - 1) continue;
                if (Activity.class.isInstance(runnable) && Activity.class.cast(runnable).iteration <= 1) continue;
                Time.sleep(pauseMillis, 2 * pauseMillis + 1);
            }

            ++iteration;
        }
    }

    @Override
    public String toString() {
        return getFullName().orElse("Unnamed activity");
    }

    /**
     * Performs a given activity after the current activity.
     */
    public Activity andThen(Activity other) {
        return Activity.newBuilder()
                .addSubActivity(this)
                .addSubActivity(other)
                .onlyOnce()
                .withoutPausingBetweenActivities()
                .build();
    }

    /**
     * Performs a given activity before the current activity.
     */
    public Activity addPreActivity(Activity other) {
        return other.andThen(this);
    }

    /**
     * Builder for building immutable Activity objects.
     */
    public static class Builder {
        private final List<BooleanSupplier> preConditions;
        private final List<Runnable> activities;
        private int maxIterations;
        private Duration maxDuration;
        private Optional<String> name;
        private Optional<ActivityConfigModel> configModel;
        private long pauseMillis; // Pause between activities will be [pauseMillis, 2 * pauseMillis]

        Builder() {
            this.preConditions = new ArrayList<>();
            this.activities = new ArrayList<>();
            this.maxIterations = 1;
            this.maxDuration = Duration.ofHours(6);
            this.name = Optional.empty();
            this.configModel = Optional.empty();
            this.pauseMillis = 333;
        }

        /**
         * Builds the completed, immutable Activiity object.
         */
        public Activity build() {
            Activity activity = new Activity(preConditions, activities, name, pauseMillis, configModel);

            activity.preConditions.add(() -> !activity.stopWatch.exceeds(maxDuration));
            activity.preConditions.add(() -> activity.iteration <= maxIterations);
            activity.preConditions.add(() -> !Globals.script.isPaused());
            activity.preConditions.add(() -> !Globals.script.isStopping());
            activity.preConditions.add(() -> Game.isLoggedIn());

            return activity;
        }

        /**
         * Adds an individual task to this activity, which will be performed in order based on the order tasks are
         * added by this method.
         */
        public Builder addSubActivity(Runnable activity) {
            activities.add(activity);
            return this;
        }

        /**
         * Adds a subactivity, taking in a keval set of params set in the GUI.
         */
        public Builder addSubActivity(Consumer<Map<String, String>> function) {
            activities.add(() -> {
                Map<String, String> map = configModel.map(model -> model.keyValStore).orElse(new HashMap<>());
                function.accept(map);
            });
            return this;
        }

        /**
         * A config model specifies how the UI will appear to a user under their selected activity. This only makes
         * sense to set on a top level activity, as that is what determines the GUI options.
         */
        public Builder withConfigModel(ActivityConfigModel configModel) {
            this.configModel = Optional.of(configModel);
            return this;
        }

        /**
         * Adds a name to this activity, just used for logging and optional to include.
         */
        public Builder withName(String name) {
            this.name = Optional.of(name);
            return this;
        }

        public Builder withPauseOf(Duration duration) {
            this.pauseMillis = duration.toMillis();
            return this;
        }

        /**
         * Removes the ~1 tick pause between sub-activities. This should only be used in cases where speed is of the
         * top importance (think prayer switches, repeated clicks on a single object, etc.) where it could be assumed
         * a player could theoretically perform the actions without pauses. As a rule of thumb, if its a common thing
         * people use hotkeys for, its probably good to use this method for it.
         */
        public Builder withoutPausingBetweenActivities() {
            return withPauseOf(Duration.ofMillis(0));
        }

        /**
         * Actions will continue to occur some prereq added by this method is false.
         */
        public Builder addPreReq(BooleanSupplier condition) {
            preConditions.add(condition);
            return this;
        }

        /**
         * Adds a prereq, taking in config information a user specified in the GUI.
         */
        public Builder addPreReq(Function<Map<String, String>, Boolean> function) {
            preConditions.add(() -> {
                Map<String, String> map = configModel.map(model -> model.keyValStore).orElse(new HashMap<>());
                return function.apply(map);
            });
            return this;
        }

        /**
         * Sets the maximum amount of time an Activity will run for. This check is performed before each action iteration,
         * so actions with long iteration times may go well beyond the given duration.
         * <p>
         * By default, activities run until a precondition fails.
         */
        public Builder maximumDuration(Duration duration) {
            maxDuration = duration;
            return this;
        }

        /**
         * Sets the maximum number of iterations an Activity will run for.
         * <p>
         * By default, activities run until a precondition fails.
         */
        public Builder maximumTimes(int count) {
            maxIterations = count;
            return this;
        }

        /**
         * Adds an activity to pause for ~600ms.
         */
        public Builder tick() {
            return addSubActivity(() -> Time.sleep(600, 614));
        }

        public Builder onlyIfAtPosition(Position pos) {
            return addPreReq(() -> Players.getLocal().getPosition().equals(pos));
        }

        public Builder onlyIfInArea(Area area) {
            return addPreReq(() -> area.contains(Players.getLocal().getPosition()));
        }

        public Builder completeAnimation() {
            sleepUntil(() -> Players.getLocal().isAnimating());
            return sleepUntil(() -> Players.getLocal().isAnimating());
        }

        public Builder thenSleepWhile(BooleanSupplier condition) {
            return thenSleepWhile(condition, Duration.ofMinutes(2));
        }

        public Builder thenSleepWhile(BooleanSupplier condition, Duration maxDuration) {
            return addSubActivity(Activity.newBuilder()
                    .addPreReq(condition)
                    .addSubActivity(() -> Time.sleep(50, 55))
                    .maximumDuration(maxDuration)
                    .untilPreconditionsFail()
                    .build());
                    // .andThen(Activities.stopScriptIf(condition)));
        }

        public Builder thenSleepUntil(BooleanSupplier condition, Duration maxDuration) {
            return thenSleepWhile(Predicates.not(condition), maxDuration);
        }

        /**
         * Another name for `thenSleepUntil`.
         */
        public Builder sleepUntil(BooleanSupplier condition) {
            return thenSleepUntil(condition);
        }

        /**
         * Waits for up to 2 minutes for a condition to become true, checking every ~50 ms. If the condition is not
         * true after 2 minutes, the program will termin8.
         */
        public Builder thenSleepUntil(BooleanSupplier condition) {
            return thenSleepUntil(condition, Duration.ofMinutes(2));
        }

        /**
         * Another name for `thenPauseFor`
         */
        public Builder pauseFor(Duration duration) {
            return thenPauseFor(duration);
        }

        /**
         * Sleeps for a duration, roughly. To keep Jagex on its toes, the actual duration waited for is randomly between
         * the input duration and 20% longer than that duration.
         */
        public Builder thenPauseFor(Duration duration) {
            return addSubActivity(Activity.newBuilder()
                    .addSubActivity(() -> Time.sleep(duration.toMillis(), duration.toMillis() * 120 / 100))
                    .build());
        }

        /**
         * Continues to repeat this activity until some other precondition fails.
         */
        public Builder untilPreconditionsFail() {
            return maximumTimes(Integer.MAX_VALUE);
        }

        /**
         * Ensures an activity is only run once.
         * <p>
         * By default, activities run until a precondition fails.
         */
        public Builder onlyOnce() {
            return maximumTimes(1);
        }
    }
}
