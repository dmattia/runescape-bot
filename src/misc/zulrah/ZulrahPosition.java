package misc.zulrah;

import org.rspeer.runetek.api.movement.position.InstancePosition;
import org.rspeer.runetek.api.scene.Npcs;

import java.util.Arrays;

public enum ZulrahPosition {
    UNKNOWN(null),
    EAST(new InstancePosition(174, 97, 0)),
    NORTH(new InstancePosition(164, 99, 0)),
    SOUTH(new InstancePosition(164, 88, 0)),
    WEST(new InstancePosition(154, 97, 0));

    public static ZulrahPosition classify() {
        if (Npcs.getNearest("Zulrah") == null) return ZulrahPosition.UNKNOWN;

        return Arrays.stream(values())
                .filter(zulrahPos -> Npcs.getNearest("Zulrah").getPosition().toInstance().equals(zulrahPos.pos))
                .findFirst()
                .orElse(ZulrahPosition.UNKNOWN);
    }

    private InstancePosition pos;

    ZulrahPosition(InstancePosition pos) {
        this.pos = pos;
    }
}