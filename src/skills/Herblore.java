package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import util.Activities;
import util.common.Activity;

public class Herblore {
    public static Activity cleanHerbs() {
        return Activity.newBuilder()
                .withName("Cleaning some ranarr")
                .addSubActivity(Activities.withdrawEqualOf("Grimy ranarr weed", "Vial of Water"))
                .addSubActivity(Activities.consumeAll("Grimy ranarr weed"))
                .addSubActivity(Activities.use("Ranarr weed", "Vial of Water"))
                .addSubActivity(Activities.produceAll("Ranarr potion (unf)"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Ranarr weed"), 1000 * 60))
                .maximumTimes(28)
                .build();
    }
}
