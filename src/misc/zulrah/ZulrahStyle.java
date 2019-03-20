package misc.zulrah;

import org.rspeer.runetek.api.component.tab.Prayer;
import org.rspeer.runetek.api.component.tab.Prayers;
import org.rspeer.runetek.api.scene.Npcs;
import util.Activities;
import util.common.Activity;

import java.util.Arrays;

public enum ZulrahStyle {
    UNKNOWN(-1),
    RANGED(2042),
    MAGE(2044),
    MELEE(2043);

    private static Activity mageSwitch() {
        return Activity.newBuilder()
                .withName("Mage switch")
                /*
                .addPreReq(() -> !Prayers.isActive(Prayer.AUGURY))
                .addSubActivity(Activities.unpray(Prayer.EAGLE_EYE))
                .addSubActivity(Activities.pray(Prayer.AUGURY))
                */
                .addSubActivity(Activities.equip("Eternal boots"))
                .addSubActivity(Activities.equip("Void mage helm"))
                .addSubActivity(Activities.equip("Occult necklace"))
                .addSubActivity(Activities.equip("Trident of the seas"))
                .addSubActivity(Activities.equip("Mage's book"))
                .addSubActivity(Activities.equip("Imbued saradomin cape"))
                .withoutPausingBetweenActivities()
                .build();
    }

    private static Activity rangeSwitch() {
        return Activity.newBuilder()
                .withName("Range switch")
                /*
                .addPreReq(() -> !Prayers.isActive(Prayer.EAGLE_EYE))
                .addSubActivity(Activities.unpray(Prayer.AUGURY))
                .addSubActivity(Activities.pray(Prayer.EAGLE_EYE))
                */
                .addSubActivity(Activities.equip("Pegasian boots"))
                .addSubActivity(Activities.equip("Toxic blowpipe"))
                .addSubActivity(Activities.equip("Necklace of anguish"))
                .addSubActivity(Activities.equip("Void ranger helm"))
                .addSubActivity(Activities.equip("Ava's accumulator"))
                .withoutPausingBetweenActivities()
                .build();
    }

    public static ZulrahStyle classify() {
        if (Npcs.getNearest("Zulrah") == null) return ZulrahStyle.UNKNOWN;

        return Arrays.stream(values())
                .filter(style -> style.id == Npcs.getNearest("Zulrah").getId())
                .findFirst()
                .orElse(ZulrahStyle.UNKNOWN);
    }

    private int id;

    ZulrahStyle(int id) {
        this.id = id;
    }
}
