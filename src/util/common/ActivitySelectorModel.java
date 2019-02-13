package util.common;

import java.util.Map;
import java.util.Optional;

public abstract class ActivitySelectorModel {
    private Activity activityToRun;

    public abstract Map<String, Activity> getActivityMap();

    private static final Activity DEFAULT_ACTIVITY = Activity.newBuilder()
            .addSubActivity(() -> System.out.println("Invalid activity selected"))
            .build();

    void setActivityByName(String activityName) {
        System.out.println("Switching to activity " + activityName);
        this.activityToRun = getActivityMap().getOrDefault(activityName, DEFAULT_ACTIVITY);
        System.out.println(activityToRun);
    }

    String[] getActivityKeys() {
        return getActivityMap().keySet().stream().sorted().toArray(String[]::new);
    }

    Optional<Activity> getActivity() {
        return Optional.ofNullable(activityToRun);
    }
}
