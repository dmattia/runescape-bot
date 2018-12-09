import org.rspeer.script.ScriptMeta;
import skills.Thieving;
import util.common.Activity;
import util.common.ActivityScript;

@ScriptMeta(developer = "AjorahAhai", desc = "Plays the game, performing quests", name = "Game Player")
public class AjorahAhaiScript extends ActivityScript {
    /**
     * Main method, does nothing interesting. Only exists because I have this script set as the main class.
     */
    public static void main(String[] args) {
        System.out.println("Built and ran!");
    }

    @Override
    public Activity getActivity() {
        return Thieving.fruitStall();
    }
}
