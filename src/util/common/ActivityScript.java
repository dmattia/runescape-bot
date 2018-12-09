package util.common;

import org.rspeer.runetek.api.commons.math.Random;
import org.rspeer.script.Script;
import util.Globals;

public abstract class ActivityScript extends Script {
    public abstract Activity getActivity();

    @Override
    public void onStart() {
        Globals.script = this;
    }

    @Override
    public int loop() {
        getActivity().run();

        return Random.nextInt(600, 2500);
    }
}
