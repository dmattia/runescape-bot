import org.rspeer.script.ScriptMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skills.*;
import util.common.Activity;
import util.common.ActivityScript;
import util.common.ActivitySelectorModel;
import util.common.ActivitySelectorView;

import javax.swing.*;

@ScriptMeta(developer = "AjorahAhai", desc = "Plays the game, performing quests", name = "Game Player")
public class AjorahAhaiScript extends ActivityScript {
    private static final Logger logger = LoggerFactory.getLogger(AjorahAhaiScript.class);

    /**
     * Main method, does nothing interesting. Only exists because I have this script set as the main class.
     */
    public static void main(String[] args) {
        logger.info("Built and ran!");
    }

    /*
    @Override
    public JFrame getGui() {
        return new ActivitySelectorView();
    }
    */

    public ActivitySelectorModel getSelectionModel() {
        return new MainActivitySelectorModel();
    }

    /*
    @Override
    public Activity getActivity() {
        //return Kitten.huntForSpices();
        //return Fishing.fishLureSpot();

        return Combat.nmz().andThen(Activity.of(() -> setStopping(true)));

        //return Prayer.getCalcium();

        //return Construction.makeOakLarder();

        //return Runecrafting.craftRunes(Runecrafting.Type.COSMIC);
        //return Mage.bakePie("Admiral pie");
        //return Mage.superGlassMake();
        //return Mage.castOn(Spell.Modern.HIGH_LEVEL_ALCHEMY, "Gold bracelet");

        //return Runecrafting.mudRunes();
        //return Runecrafting.steamRunes();
        //return Runecrafting.astralRunes();
        //return Agility.werewolf();

        //return Thieving.masterGardner();
    }
    */
}
