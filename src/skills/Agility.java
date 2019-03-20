package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.tab.EquipmentSlot;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.movement.transportation.FairyRing;
import org.rspeer.runetek.api.scene.*;
import util.Activities;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.Arrays;
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

        Activity goHome = Activity.newBuilder()
                .withName("Going home")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                // Only if haven't already gotten near trapdoor
                .addPreReq(() -> !Area.rectangular(3537, 3510, 3609, 3453).contains(Players.getLocal().getPosition()))
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").click())
                .thenSleepUntil(House::isInside)
                .tick()
                .build();

        Activity getToTrapdoor = Activity.newBuilder()
                .withName("Moving to trapdoor")
                .onlyIfInArea(Area.rectangular(3537, 3510, 3609, 3453))
                .addSubActivity(Activities.moveTo(new Position(3544, 3466)))
                .build();

        Activity goDownTrapdoor = Activity.newBuilder()
                .withName("Going down trapdoor")
                .addPreReq(() -> SceneObjects.getNearest("Trapdoor") != null)
                .onlyIfInArea(Area.rectangular(3540, 3467, 3547, 3458))
                .addSubActivity(() -> SceneObjects.getNearest("Trapdoor").interact("Open"))
                .sleepUntil(() -> SceneObjects.getNearest("Trapdoor").containsAction("Climb-down"))
                .tick()
                .addSubActivity(() -> SceneObjects.getNearest("Trapdoor").interact("Climb-down"))
                .sleepUntil(Dialog::isOpen)
                .addSubActivity(() -> Dialog.processContinue())
                .tick()
                .build();

        Activity getToCourseUsingFairyRing = Activity.newBuilder()
                .addPreReq(() -> !Area.rectangular(3518, 9918, 3558, 9853).contains(Players.getLocal().getPosition()))
                .addSubActivity(goHome)
                .addSubActivity(Activities.useHouseFairyRing(FairyRing.Destination.ALQ))
                .addSubActivity(getToTrapdoor)
                .addSubActivity(goDownTrapdoor)
                .build();

        Activity moveToStart = Activity.newBuilder()
                .withName("Moving to start position")
                .onlyIfInArea(Area.polygonal(
                        new Position(3519, 9899, 0),
                        new Position(3519, 9857, 0),
                        new Position(3549, 9857, 0),
                        new Position(3552, 9866, 0),
                        new Position(3550, 9871, 0),
                        new Position(3532, 9867, 0),
                        new Position(3533, 9899, 0)
                ))
                .addPreReq(() -> !Inventory.contains("Stick"))
                .addSubActivity(Activities.moveToExactly(start))
                .thenPauseFor(Duration.ofSeconds(1))
                .build();

        Activity steppingStones = steppingStonePositions.stream()
                .map(pos -> SceneObjects.getAt(pos))
                .flatMap(Arrays::stream)
                .filter(sceneObject -> sceneObject.getName().equals("Stepping stone"))
                .map(stone -> Activity.newBuilder()
                        .withName("Stepping a stone")
                        .addPreReq(() -> stone != null)
                        .addSubActivity(() -> stone.interact("Jump-to"))
                        .thenSleepUntil(() -> Players.getLocal().getPosition().equals(stone.getPosition()),
                                Duration.ofSeconds(2))
                        .build())
                .collect(new ActivityCollector())
                .addPreReq(() -> !Inventory.contains("Stick"))
                .onlyIfInArea(Area.rectangular(3534, 9883, 3547, 9870))
                .build();

        Activity hurdles = hurdlePositions.stream()
                .map(pos -> SceneObjects.getAt(pos))
                .flatMap(Arrays::stream)
                .filter(sceneObject -> sceneObject.getName().equals("Hurdle"))
                .map(hurdle -> Activity.newBuilder()
                        .withName("Jumping hurdle")
                        .addPreReq(() -> Players.getLocal().getY() < hurdle.getPosition().getY())
                        .addSubActivity(() -> hurdle.click())
                        .thenSleepUntil(() -> Players.getLocal().getY() > hurdle.getPosition().getY())
                        .build())
                .collect(new ActivityCollector())
                .onlyIfInArea(Area.rectangular(3533, 9902, 3548, 9881))
                .addPreReq(() -> !Inventory.contains("Stick"))
                .build();

        Activity pipe = Activity.newBuilder()
                .withName("Piping it")
                .addPreReq(() -> SceneObjects.getNearest("Pipe") != null)
                .addPreReq(() -> !Inventory.contains("Stick"))
                .onlyIfInArea(Area.rectangular(3532, 9906, 3548, 9898))
                .addSubActivity(() -> SceneObjects.getNearest("Pipe").click())
                // The pipe is actually 2 separate animations, one that completes inside the pipe
                .completeAnimation()
                .completeAnimation()
                .pauseFor(Duration.ofSeconds(3))
                .tick()
                .build();

        Activity eatIfNeeded = Activity.newBuilder()
                .withName("Eating some brew")
                .addPreReq(() -> Health.getCurrent() < 55)
                .addPreReq(() -> Inventory.contains(SARA_BREW))
                .addPreReq(() -> !Inventory.contains("Stick"))
                .addSubActivity(() -> Inventory.getFirst(SARA_BREW).click())
                .tick()
                .untilPreconditionsFail()
                .build();

        Activity takeStick = Activity.newBuilder()
                .withName("Taking stick")
                .onlyIfInArea(Area.rectangular(3533, 9917, 3548, 9907))
                .addPreReq(() -> Pickables.getNearest("Stick") != null)
                .addPreReq(() -> !Inventory.contains("Stick"))
                .addSubActivity(() -> Pickables.getNearest("Stick").interact("Take"))
                .thenSleepWhile(() -> !Inventory.contains("Stick"), Duration.ofSeconds(10))
                .build();

        Activity skullSlope = Activity.newBuilder()
                .withName("Climbing Skull slope")
                .addPreReq(() -> SceneObjects.getNearest("Skull slope") != null)
                .addPreReq(() -> Inventory.contains("Stick"))
                .onlyIfInArea(Area.rectangular(3529, 9918, 3551, 9906))
                .addSubActivity(() -> SceneObjects.getNearest("Skull slope").interact("Climb-up"))
                .thenSleepUntil(() -> Players.getLocal().getX() < 3531)
                .build();

        Activity zipLine = Activity.newBuilder()
                .withName("Zipping line")
                .addPreReq(() -> SceneObjects.getNearest("Zip line") != null)
                .addPreReq(() -> Inventory.contains("Stick"))
                .onlyIfInArea(Area.rectangular(3523, 9915, 3533, 9904))
                .addSubActivity(() -> SceneObjects.getNearest("Zip line").click())
                .thenSleepWhile(() -> Players.getLocal().getAnimation() == -1)
                .thenSleepUntil(() -> Players.getLocal().getAnimation() == -1)
                .build();

        Activity giveStick = Activity.newBuilder()
                .withName("Giving stick")
                .addPreReq(() -> Inventory.contains("Stick"))
                .onlyIfInArea(Area.rectangular(3522, 9899, 3532, 9865))
                .addSubActivity(Activities.moveTo(trainer))
                .addSubActivity(() -> Npcs.getNearest("Agility Trainer").click())
                .thenSleepWhile(() -> Inventory.contains("Stick"))
                .build();

        Activity handleInsideOfCourse = Activity.newBuilder()
                .withName("Running werewolf agility course")
                .addPreReq(() -> Inventory.contains(SARA_BREW))
                .addPreReq(() -> !Inventory.isFull() || Inventory.contains("Stick"))
                .onlyIfInArea(Area.rectangular(3518, 9918, 3558, 9853))
                .addSubActivity(eatIfNeeded)
                .addSubActivity(Activities.toggleRun())
                .addSubActivity(moveToStart)
                .addSubActivity(steppingStones)
                .addSubActivity(hurdles)
                .addSubActivity(pipe)
                .addSubActivity(takeStick)
                .addSubActivity(skullSlope)
                .addSubActivity(zipLine)
                .addSubActivity(giveStick)
                .build();

        return Activity.newBuilder()
                .addPreReq(() -> Inventory.contains(SARA_BREW))
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("charos"))
                .addPreReq(() -> !Inventory.isFull() || Inventory.contains("Stick"))
                .addSubActivity(getToCourseUsingFairyRing)
                .addSubActivity(handleInsideOfCourse)
                .withoutPausingBetweenActivities()
                .build();
    }
}
