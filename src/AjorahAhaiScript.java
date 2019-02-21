import org.rspeer.script.ScriptMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skills.*;
import util.common.Activity;
import util.common.ActivityScript;
import util.common.ActivitySelectorModel;

@ScriptMeta(developer = "AjorahAhai", desc = "Plays the game, performing quests", name = "Game Player")
public class AjorahAhaiScript extends ActivityScript {
    private static final Logger logger = LoggerFactory.getLogger(AjorahAhaiScript.class);

    /**
     * Main method, does nothing interesting. Only exists because I have this script set as the main class.
     */
    public static void main(String[] args) {
        logger.info("Built and ran!");
    }

    public ActivitySelectorModel getSelectionModel() {
        return new MainActivitySelectorModel();
    }
}
