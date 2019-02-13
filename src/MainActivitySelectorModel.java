import misc.Kitten;
import org.rspeer.runetek.api.component.tab.Spell;
import skills.*;
import util.common.Activity;
import util.common.ActivitySelectorModel;

import java.util.HashMap;
import java.util.Map;

public class MainActivitySelectorModel extends ActivitySelectorModel {
    private static final Map<String, Activity> ACTIVITY_MAP = new HashMap<String, Activity>() {{
        put("Fight in nmz", Combat.nmz());
        put("RC Astrals", Runecrafting.astralRunes());
        put("RC Steams", Runecrafting.steamRunes());
        put("RC Mud", Runecrafting.mudRunes());
        put("Werewolf agility", Agility.werewolf());
        put("Superglass Make", Mage.superGlassMake());
        put("Alch Gold Bracelets", Mage.castOn(Spell.Modern.HIGH_LEVEL_ALCHEMY, "Gold bracelet"));
        put("Construct Oak Larders using Phials", Construction.makeOakLarder());
        put("Hunt for spices with cat", Kitten.huntForSpices());
        put("Thieve master gardener", Thieving.masterGardner());
    }};

    @Override
    public Map<String, Activity> getActivityMap() {
        return ACTIVITY_MAP;
    }
}
