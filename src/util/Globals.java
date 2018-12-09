package util;

import org.rspeer.script.Script;

public class Globals {
    /**
     * There is no method like: Game::isPaused :(
     * In fact, inside a Script is the only place Script::isPaused will have a value, and the listeners will
     * only register for this LoopTask.
     * Because of this annoyance, I'll keep track in a publicly gettable variable to avoid having to pass this
     * script or some BooleanSupplier around all my Activity instances.
     */
    public static Script script;
}
