package skills;

import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.*;

// TODO(dmattia): THIS DOESN"T WORK!!!
public class Firemaking {
    public static Activity burnWillows() {
        Activity prepSupplies = Activity.newBuilder()
                .withName("Prepping supplies")
                .addPreReq(() -> !Inventory.contains("Willow logs"))
                .addSubActivity(Activities.withdraw("Willow logs", 27))
                .addSubActivity(Activities.closeBank())
                .onlyOnce()
                .build();

        Activity moveToOpenSpace = Activity.newBuilder()
                .withName("Moving to open space")
                .addPreReq(() -> isOnSceneObject(Players.getLocal().getPosition()))
                .addPreReq(() -> findFireSpot(3).isPresent())
                .addSubActivity(() -> {
                    Position dest = findFireSpot(3).get();
                    Movement.walkTo(dest);
                    Time.sleepUntil(() -> Players.getLocal().getPosition().equals(dest), 450, 5000);
                })
                .onlyOnce()
                .build();

        Activity burnLog = Activity.newBuilder()
                .withName("Burning a single log")
                .addPreReq(() -> Inventory.contains("Tinderbox"))
                .addPreReq(() -> Inventory.contains("Willow logs"))
                .addPreReq(() -> !isOnSceneObject(Players.getLocal().getPosition()))
                .addSubActivity(Activities.use("Tinderbox", "Willow logs"))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(1500)))
                .onlyOnce()
                .build();

        Activity burnAllLogs = Activity.newBuilder()
                .withName("Burn them all")
                .addPreReq(() -> Inventory.contains("Tinderbox"))
                .addPreReq(() -> Inventory.contains("Willow logs"))
                .addSubActivity(moveToOpenSpace)
                .addSubActivity(burnLog)
                .untilPreconditionsFail()
                .build();

        Activity all = Activity.newBuilder()
                .withName("Running loop")
                .addSubActivity(prepSupplies)
                .addSubActivity(burnAllLogs)
                .build();

        Activity tmp = Activity.newBuilder()
                .addSubActivity(() -> {
                    for (SceneObject obj : SceneObjects.getLoaded()) {
                        System.out.println(obj.getName());
                    }
                })
                .build();

        return all;
    }

    private static boolean isOnSceneObject(Position pos) {
        Set<String> sceneObjNames = new HashSet<>(Arrays.asList("Fire", "Ashes"));

        return Arrays.stream(SceneObjects.getLoaded())
                .filter(sceneObject -> sceneObjNames.contains(sceneObject.getName()))
                .map(SceneObject::getPosition)
                .anyMatch(sceneObjPos -> pos.equals(sceneObjPos));
    }

    private static Optional<Position> findFireSpot(int maxDistance) {
        Position current = Players.getLocal().getPosition();

        List<Position> positions = new ArrayList<>();
        for (int i = -maxDistance; i <= maxDistance; i++) {
            for (int j = -maxDistance; j <= maxDistance; j++) {
                positions.add(new Position(current.getX() + i, current.getY() + j));
            }
        }

        return positions.stream()
                .filter(Movement::isWalkable)
                .filter(pos -> isOnSceneObject(pos))
                .sorted((pos1, pos2) -> pos1.distance(Players.getLocal()) < pos2.distance(Players.getLocal()) ? -1 : 1)
                .findFirst();
    }
}
