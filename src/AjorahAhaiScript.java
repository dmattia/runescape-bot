import org.rspeer.runetek.adapter.Varpbit;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Prayer;
import org.rspeer.runetek.api.component.tab.Prayers;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.event.listeners.BankLoadListener;
import org.rspeer.runetek.event.listeners.VarpListener;
import org.rspeer.script.ScriptMeta;
import skills.*;
import util.Activities;
import util.Globals;
import util.common.Activity;
import util.common.ActivityCollector;
import util.common.ActivityScript;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

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
        //return Combat.nmz().andThen(Activity.of(() -> setStopping(true)));

        return Runecrafting.craftRunes(Runecrafting.Type.NATURE);
    }
}
