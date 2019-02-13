package util.common;

import org.rspeer.runetek.api.commons.math.Random;
import org.rspeer.script.Script;
import util.Globals;

public abstract class ActivityScript extends Script {
    ActivitySelectorModel model;
    ActivitySelectorView view;

    public abstract ActivitySelectorModel getSelectionModel();

    @Override
    public void onStart() {
        model = getSelectionModel();
        view = new ActivitySelectorView(model);
        view.setVisible(true);

        Globals.script = this;
    }

    @Override
    public int loop() {
        model.getActivity().ifPresent(Activity::run);

        return Random.nextInt(220, 350);
    }
}
