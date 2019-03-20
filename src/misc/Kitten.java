package misc;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.scene.Npcs;
import util.Activities;
import util.common.Activity;

import java.time.Duration;

public class Kitten {
    public static Activity feed(String fishName) {
        return Activity.newBuilder()
                .withName("Feeding kitten")
                .addPreReq(() -> Npcs.getNearest("Kitten") != null)
                .addPreReq(() -> Inventory.contains(fishName))
                .addSubActivity(Activities.use(fishName))
                .addSubActivity(() -> Npcs.getNearest("Kitten").interact("Use"))
                .thenPauseFor(Duration.ofSeconds(4))
                .build();
    }

    public static Activity pet() {
        return Activity.newBuilder()
                .withName("Petting kitten")
                .addPreReq(() -> Npcs.getNearest("Kitten") != null)
                .addSubActivity(() -> Npcs.getNearest("Kitten").interact("Interact"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 4000))
                .addSubActivity(() -> Dialog.process(0))
                .addSubActivity(() -> Time.sleepWhile(Dialog::isOpen, 4000))
                .thenPauseFor(Duration.ofMillis(4567))
                .build();
    }

    public static Activity huntForSpices() {
        return Activity.newBuilder()
                .addSubActivity(() -> Npcs.getNearest("Hellcat").interact("Interact"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 4000))
                .addSubActivity(() -> Dialog.process(1))
                .addSubActivity(() -> Time.sleepWhile(Dialog::isOpen, 4000))
                .thenPauseFor(Duration.ofSeconds(5))
                .addSubActivity(Activities.pickup("Orange spice ("))
                .build();
    }
}
