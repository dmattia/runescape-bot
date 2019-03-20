package skills;

import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.ui.Log;
import util.Activities;
import util.Globals;
import util.common.Activity;
import util.common.ActivityConfigModel;

import java.util.Arrays;

public class Smithing {
    public static Activity smith() {
        ActivityConfigModel configModel = ActivityConfigModel.newBuilder()
                .withTextField("Bar", "Mithril bar")
                .withTextField("Item", "Mithril platebody")
                .build();

        Activity getBars = Activity.newBuilder()
                .withName("Getting bars from bank")
                .withConfigModel(configModel)
                // .onlyIfInArea(Area.rectangular(3183, 3428, 3191, 3419))
                .addPreReq(config -> Inventory.contains(config.get("Item")))
                .addPreReq(config -> !Inventory.contains(config.get("Bar")))
                .addSubActivity(Activities.openBank())
                .addSubActivity(config -> Activities.depositAll(config.get("Item")).run())
                .addSubActivity(config -> Activities.withdraw(config.get("Bar"), 25).run())
                .addSubActivity(Activities.closeBank())
                .build();

        Activity smelt = Activity.newBuilder()
                .withName("Smithing items on anvil")
                .withConfigModel(configModel)
                // .onlyIfInArea(Area.rectangular(3179, 3448, 3191, 3432))
                .addPreReq(config -> Inventory.contains(config.get("Bar")))
                .addSubActivity(config -> Inventory.getFirst(config.get("Bar")).interact("Use"))
                .addSubActivity(() -> SceneObjects.getNearest("Anvil").interact("Smith"))
                .thenSleepUntil(() -> Interfaces.isOpen(312))
                .addSubActivity(config -> {
                    Arrays.stream(Interfaces.get(312))
                            .filter(component -> component.getName().contains(config.get("Item")))
                            .findFirst()
                            .ifPresent(component -> component.interact("Smith All"));
                    Time.sleepUntil(() -> {
                        return !Inventory.contains(config.get("Bar")) || Dialog.isOpen();
                    }, 1000 * 20);
                })
                .build();

        return Activity.newBuilder()
                .withName("Smithing")
                .withConfigModel(configModel)
                .addPreReq(() -> Inventory.contains("Hammer"))
                .addSubActivity(getBars)
                .addSubActivity(smelt)
                .withoutPausingBetweenActivities()
                .build();
    }
}
