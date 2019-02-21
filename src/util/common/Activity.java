package util.common;

import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.StopWatch;
import org.rspeer.runetek.api.commons.Time;
import util.Globals;
import org.rspeer.ui.Log;

import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Activities are composable units of work that are set to automatically have anti-ban measures between tasks.
 * In addition to making some in game action, activities can also specify their lifespans, conditions to run, and
 * relationships to other activities.
 * <p>
 * A single activity is not thread safe, and should only ever be performed once at a time.
 * <p>
 * TODO(dmattia): It should be possible to specify that an activity randomly selects from a set of activities.
 */
public class Activity implements Runnable {
    private final List<BooleanSupplier> preConditions;
    private final List<Runnable> activities;
    private final StopWatch stopWatch;
    private int iteration;
    private Optional<String> name;
    private Optional<Activity> parent;
    private boolean pauseBetweenActivities;
    private Optional<ActivityConfigModel> configModel;

    // private because you should only create activities through Activity.newBuilder()...build().
    private Activity(List<BooleanSupplier> preConditions,
                     List<Runnable> activities,
                     Optional<String> name,
                     boolean pauseBetweenActivities,
                     Optional<ActivityConfigModel> configModel) {
        this.preConditions = preConditions;
        this.activities = activities;
        this.stopWatch = StopWatch.start();
        this.iteration = 1;
        this.name = name;
        this.parent = Optional.empty();
        this.pauseBetweenActivities = pauseBetweenActivities;
        this.configModel = configModel;

        activities.stream()
                .filter(Activity.class::isInstance)
                .map(Activity.class::cast)
                .map(child -> child.setParent(this));
    }

    private Activity setParent(Activity parent) {
        this.parent = Optional.of(parent);
        return this;
    }

    public Optional<ActivityConfigModel> getConfigModel() {
        return configModel;
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
                if (pauseBetweenActivities) Time.sleep(333, 666);
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
        private boolean pauseBetweenActivities;
        private Optional<ActivityConfigModel> configModel;

        Builder() {
            this.preConditions = new ArrayList<>();
            this.activities = new ArrayList<>();
            this.maxIterations = 1;
            this.maxDuration = Duration.ofHours(6);
            this.name = Optional.empty();
            this.pauseBetweenActivities = true;
            this.configModel = Optional.empty();
        }

        /**
         * Builds the completed, immutable Activiity object.
         */
        public Activity build() {
            Activity activity = new Activity(preConditions, activities, name, pauseBetweenActivities, configModel);

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

        public Builder addSubActivity(Consumer<Map<String, String>> function) {
            activities.add(() -> {
                Map<String, String> map = configModel.map(model -> model.keyValStore).orElse(new HashMap<>());
                function.accept(map);
            });
            return this;
        }

        /*
        public Builder addSubActivity(Function<Map<String, String>, Activity> function) {
            activities.add(() -> {
                Map<String, String> map = configModel.map(model -> model.keyValStore).orElse(new HashMap<>());
                function.apply(map).run();
            });
            return this;
        }
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

        public Builder withoutPausingBetweenActivities() {
            this.pauseBetweenActivities = false;
            return this;
        }

        /**
         * Actions will continue to occur some prereq added by this method is false.
         * TODO(dmattia): This needs a better name, as it is an OR'd check between every iteration, not just an initial check.
         */
        public Builder addPreReq(BooleanSupplier condition) {
            preConditions.add(condition);
            return this;
        }

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
