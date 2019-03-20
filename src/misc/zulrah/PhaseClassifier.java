package misc.zulrah;

import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

public class PhaseClassifier {
    static Stack<ZulrahState> stateHistory = new Stack<>();

    public static void reset() {
        stateHistory.clear();
    }

    /**
     * Checks zulrah's current status, updating the state history if zulrah has recently changed to a new form/position.
     * Returns the complete history of states zulrah has been in.
     */
    static List<ZulrahState> checkPhase() {
        ZulrahState currentState = ZulrahState.classify();
        if (!currentState.isValid()) return stateHistory;

        if (stateHistory.isEmpty() || !stateHistory.peek().equals(currentState)) {
            stateHistory.push(currentState);
        }

        return stateHistory;
    }
}
