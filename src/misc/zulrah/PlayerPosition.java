package misc.zulrah;

import org.rspeer.runetek.adapter.Positionable;
import org.rspeer.runetek.api.movement.position.InstancePosition;
import org.rspeer.runetek.api.movement.position.Position;

public enum PlayerPosition {
    START(164, 92),
    NORTH_EAST_1(170, 101),
    NORTH_EAST_2(168, 102),
    NORTH_WEST(159, 102),
    SE_PILLAR_OUTSIDE(168, 95),
    SE_PILLAR_INSIDE(166, 94),
    SW_PILLAR_OUTSIDE(160, 96);

    // Start is Instance 164, 92
    // First spot is Instance 170, 101
    // Melee spot after first is 168, 102
    // Zulrah starts @ Instance 164, 99

    /*
            case WEST:
                return new WorldPoint(startTile.getX() - 5, startTile.getY(), 0);
            case EAST:
                return new WorldPoint(startTile.getX() + 5, startTile.getY() - 2, 0);
            case SOUTH:
                return new WorldPoint(startTile.getX(), startTile.getY() - 6, 0);
            case SOUTH_WEST:
                return new WorldPoint(startTile.getX() - 4, startTile.getY() - 4, 0);
            case SOUTH_EAST:
                return new WorldPoint(startTile.getX() + 2, startTile.getY() - 6, 0);
            case TOP_EAST:
                return new WorldPoint(startTile.getX() + 6, startTile.getY() + 2, 0);
            case TOP_WEST:
                return new WorldPoint(startTile.getX() - 4, startTile.getY() + 3, 0);
            case PILLAR_WEST_INSIDE:
                return new WorldPoint(startTile.getX() - 4, startTile.getY() - 3, 0);
            case PILLAR_WEST_OUTSIDE:
                return new WorldPoint(startTile.getX() - 5, startTile.getY() - 3, 0);
            case PILLAR_EAST_INSIDE:
                return new WorldPoint(startTile.getX() + 4, startTile.getY() - 3, 0);
            case PILLAR_EAST_OUTSIDE:
                return new WorldPoint(startTile.getX() + 4, startTile.getY() - 4, 0);
     */
    Positionable getPosition() {
        return pos;
    }

    private InstancePosition pos;

    PlayerPosition(int x, int y) {
        this.pos = new InstancePosition(x, y, 0);
    }
}
