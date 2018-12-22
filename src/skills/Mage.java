package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.*;
import util.Activities;
import util.common.Activity;

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

    public static Activity castOn(Spell spell, String inventoryItem) {
        return Activity.newBuilder()
                .withName("Casting spell " + spell.toString() + " on "  + inventoryItem)
                .addPreReq(() -> Inventory.contains(inventoryItem))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Modern.LEVEL_3_ENCHANT))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> {
                    int count = Inventory.getCount(inventoryItem);
                    Inventory.getFirst(inventoryItem).interact("Cast");
                    Time.sleepUntil(() -> Inventory.getCount(inventoryItem) < count, 111, 600);
                })
                .untilPreconditionsFail()
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
}
