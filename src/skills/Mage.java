package skills;

import org.rspeer.ui.Log;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.SkillListener;
import org.rspeer.runetek.event.types.SkillEvent;
import util.Activities;
import util.common.Activity;
import util.common.ActivityConfigModel;

import java.util.concurrent.atomic.AtomicBoolean;

public class Mage {
    public static Activity enchant() {
        return Activity.newBuilder()
                .withName("Enchanting topaz amulet")
                .addSubActivity(getAmulets())
                .addSubActivity(castOn(Spell.Modern.LEVEL_3_ENCHANT, "Topaz amulet"))
                .build();
    }

    public static Activity stringAndEnchant() {
        Activity getStringsAndAmulets = Activity.newBuilder()
                .withName("Getting supplies")
                .addPreReq(() -> !Inventory.contains("Topaz amulet (u)", "Topaz amulet"))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.withdraw("Cosmic rune", 13))
                .addSubActivity(Activities.withdraw("Topaz amulet (u)", 13))
                .addSubActivity(Activities.withdraw("Ball of wool", 13))
                .addSubActivity(Activities.closeBank())
                .onlyOnce()
                .build();

        Activity makeAmulets = Activity.newBuilder()
                .withName("Making amulets")
                .addPreReq(() -> Inventory.contains("Topaz amulet (u)"))
                .addPreReq(() -> Inventory.contains("Ball of wool"))
                .addSubActivity(Activities.use("Topaz amulet (u)", "Ball of wool"))
                .addSubActivity(Activities.produceAll("Topaz amulet"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Ball of wool"), 1000 * 30))
                .build();

        return Activity.newBuilder()
                .withName("String/Enchant amulets")
                .addSubActivity(getStringsAndAmulets)
                .addSubActivity(makeAmulets)
                .addSubActivity(castOn(Spell.Modern.LEVEL_3_ENCHANT, "Topaz amulet"))
                .addSubActivity(() -> Tabs.open(Tab.INVENTORY))
                .build();
    }

    public static Activity highAlch() {
        ActivityConfigModel config = ActivityConfigModel.newBuilder()
                .withTextField("alchable", "Gold bracelet")
                .build();

        return Activity.newBuilder()
                .withName("High alching")
                .withConfigModel(config)
                .addSubActivity(map -> Mage.castOn(Spell.Modern.HIGH_LEVEL_ALCHEMY, map.get("alchable")).run())
                .build();
    }

    public static Activity superGlassMake() {
        Activity getSupplies = Activity.newBuilder()
                .withName("Getting seaweed and buckets of sand")
                .addSubActivity(Activities.openBank())
                .addSubActivity(Activities.depositAll("Molten glass"))
                .addSubActivity(Activities.withdraw("Giant seaweed", 3))
                .addSubActivity(Activities.withdraw("Bucket of sand", 18))
                .addSubActivity(Activities.closeBank())
                .build();

        Activity doMagic = Activity.newBuilder()
                .withName("Casting the spell")
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.SUPERGLASS_MAKE))
                .build();

        return Activity.newBuilder()
                .withName("Casting superglass make")
                .addPreReq(() -> Inventory.getCount(true, "Air rune") > 100)
                .addPreReq(() -> Inventory.getCount(true, "Astral rune") > 10)
                .addSubActivity(getSupplies)
                .addSubActivity(doMagic)
                .build();
    }

    public static Activity castOn(Spell spell, String inventoryItem) {
        return Activity.newBuilder()
                .withName("Casting spell " + spell.toString() + " on "  + inventoryItem)
                .addPreReq(() -> Inventory.contains(inventoryItem))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(spell))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> {
                    int count = Inventory.getCount(inventoryItem);
                    Inventory.getFirst(inventoryItem).interact("Cast");
                    Time.sleepUntil(() -> Inventory.getCount(inventoryItem) < count, 111, 600);
                })
                //.untilPreconditionsFail()
                .build();
    }

    private static Activity getAmulets() {
        return Activity.newBuilder()
                .withName("Getting amulets from bank")
                .addPreReq(() -> !Inventory.contains("Topaz amulet"))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.withdraw("Cosmic rune", 27))
                .addSubActivity(Activities.withdraw("Topaz amulet", 27))
                .addSubActivity(Activities.closeBank())
                .build();
    }

    public static Activity bakePie(String pieName) {
        return Activity.newBuilder()
                .withName("Baking some sweet, sweet pie")
                .addPreReq(() -> Inventory.contains("Astral rune"))
                .addPreReq(() -> Inventory.contains("Raw " + pieName))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.BAKE_PIE))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> {
                    AtomicBoolean complete = new AtomicBoolean(false);
                    SkillListener listener = event -> {
                        if (event.getType() == SkillEvent.TYPE_LEVEL) {
                            Log.info("Advanced a level :)");
                            complete.set(true);
                        }
                    };
                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> complete.get() || !Inventory.contains("Raw " + pieName), 1000 * 50);
                    Game.getEventDispatcher().deregister(listener);
                })
                .addSubActivity(Activities.depositAll(pieName))
                .addSubActivity(Activities.withdraw("Raw " + pieName, 27))
                .addSubActivity(Activities.closeBank())
                .build();
    }
}
