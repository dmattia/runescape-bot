package util.common;

import org.rspeer.ui.Log;

import java.util.Map;
import java.util.Optional;

public abstract class ActivitySelectorModel {
    private Activity activityToRun;

    public abstract Map<String, Activity> getActivityMap();

    private static final Activity DEFAULT_ACTIVITY = Activity.newBuilder()
            .addSubActivity(() -> Log.info("Invalid activity selected"))
            .build();

    void setActivityByName(String activityName) {
        Log.info("Switching to activity " + activityName);
        this.activityToRun = getActivityMap().getOrDefault(activityName, DEFAULT_ACTIVITY);
        Log.info(activityToRun);
    }

    String[] getActivityKeys() {
        return getActivityMap().keySet().stream().sorted().toArray(String[]::new);
    }

    Optional<Activity> getActivity() {
        return Optional.ofNullable(activityToRun);
    }
}
