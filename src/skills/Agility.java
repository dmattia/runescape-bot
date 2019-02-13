package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Pickable;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.movement.position.ScenePosition;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Pickables;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.providers.RSPickable;
import util.Activities;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class Agility {
    public static Activity werewolf() {
        Position start = new Position(3538, 9873);
        Position trainer = new Position(3528, 9868);

        Predicate<Item> SARA_BREW = item -> item.getName().contains("Saradomin brew");

        List<Position> steppingStonePositions = Arrays.asList(
                new Position(3538, 9875),
                new Position(3538, 9877),
                new Position(3540, 9877),
                new Position(3540, 9879),
                new Position(3540, 9881)
        );

        List<Position> hurdlePositions = Arrays.asList(
                new Position(3539, 9893),
                new Position(3539, 9896),
                new Position(3539, 9899)
        );

        Activity steppingStones = steppingStonePositions.stream()
                .map(pos -> SceneObjects.getAt(pos))
                .flatMap(Arrays::stream)
                .filter(sceneObject -> sceneObject.getName().equals("Stepping stone"))
                .map(stone -> Activity.newBuilder()
                        .withName("Stepping a stone")
                        .addPreReq(() -> stone != null)
                        .addSubActivity(() -> stone.click())
                        .addSubActivity(Activities.sleepUntil(() -> Players.getLocal().getPosition().equals(stone.getPosition())))
                        .build())
                .collect(new ActivityCollector())
                .withoutPausingBetweenActivities()
                .build();

        Activity hurdles = hurdlePositions.stream()
                .map(pos -> SceneObjects.getAt(pos))
                .flatMap(Arrays::stream)
                .filter(sceneObject -> sceneObject.getName().equals("Hurdle"))
                .map(hurdle -> Activity.newBuilder()
                        .withName("Jumping hurdle")
                        .addPreReq(() -> Players.getLocal().getY() < hurdle.getPosition().getY())
                        .addSubActivity(() -> hurdle.click())
                        .addSubActivity(Activities.sleepUntil(
                                () -> Players.getLocal().getY() > hurdle.getPosition().getY()
                        ))
                        .build())
                .collect(new ActivityCollector())
                .withoutPausingBetweenActivities()
                .build();


        Activity takeStick = Activity.newBuilder()
                .withName("Taking stick")
                .addPreReq(() -> Pickables.getNearest("Stick") != null)
                .addPreReq(() -> !Inventory.contains("Stick"))
                .addSubActivity(() -> Pickables.getNearest("Stick").interact("Take"))
                .addSubActivity(Activities.sleepWhile(() -> !Inventory.contains("Stick")))
                .build();


        Activity pipe = Activity.newBuilder()
                .withName("Going through pipe")
                .addSubActivity(() -> SceneObjects.getNearest("Pipe").click())
                .addSubActivity(Activities.sleepUntil(() -> Players.getLocal().getY() > 9909))
                .build();

        Activity eatIfNeeded = Activity.newBuilder()
                .withName("Eating some brew")
                .addPreReq(() -> Health.getCurrent() < 55)
                .addPreReq(() -> Inventory.contains(SARA_BREW))
                .addSubActivity(() -> Inventory.getFirst(SARA_BREW).click())
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(650)))
                .untilPreconditionsFail()
                .build();

        return Activity.newBuilder()
                .withName("Running werewolf agility course")
                .addPreReq(() -> Inventory.contains(SARA_BREW))
                .addSubActivity(eatIfNeeded)
                .addSubActivity(Activities.toggleRun())
                .addSubActivity(Activities.moveToExactly(start))
                .addSubActivity(steppingStones)
                .addSubActivity(hurdles)
                .addSubActivity(pipe)
                .addSubActivity(takeStick)
                .addSubActivity(() -> SceneObjects.getNearest("Skull slope").click())
                .addSubActivity(Activities.sleepUntil(() -> Players.getLocal().getX() < 3531))
                .addSubActivity(() -> SceneObjects.getNearest("Zip line").click())
                .addSubActivity(Activities.sleepWhile(() -> Players.getLocal().getAnimation() == -1))
                .addSubActivity(Activities.sleepUntil(() -> Players.getLocal().getAnimation() == -1))
                .addSubActivity(Activities.moveTo(trainer))
                .addSubActivity(() -> Npcs.getNearest("Agility Trainer").click())
                .addSubActivity(Activities.sleepWhile(() -> Inventory.contains("Stick")))
                .build();
    }
}
