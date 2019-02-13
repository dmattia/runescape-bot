package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class Thieving {
    /**
     * I did this lvl 5 -> 25 in about an hour. There's no risk of attack.
     */
    public static Activity teaStall() {
        return Activity.newBuilder()
                .withName("Thieving from tea stall")
                .addPreReq(() -> Skills.getCurrentLevel(Skill.THIEVING) > 5)
                .addPreReq(() -> Skills.getCurrentLevel(Skill.THIEVING) < 25)
                .addSubActivity(() -> Time.sleepUntil(() -> {
                    return Optional.ofNullable(SceneObjects.getNearest("Tea stall"))
                            .map(so -> so.containsAction("Steal-from"))
                            .orElse(false);
                }, 120, 1000 * 10))
                .addSubActivity(() -> SceneObjects.getNearest("Tea stall").interact("Steal-from"))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(546)))
                .addSubActivity(Activities.dropAll("Cup of tea"))
                .untilPreconditionsFail()
                .build();
    }

    /**
     * I did this in full rune + dragon longsword + autoretaliate on so that the dogs were non issues. Could also
     * just trap them.
     */
    public static Activity fruitStall() {
        return Activity.newBuilder()
                .withName("Thieving from fruit stall")
                .addPreReq(() -> SceneObjects.getNearest("Fruit Stall") != null)
                .addSubActivity(() -> {
                    int count = Inventory.getCount();
                    SceneObjects.getNearest("Fruit Stall").interact("Steal-from");
                    Time.sleepUntil(() -> count < Inventory.getCount(), 180, 2000);
                })
                .addSubActivity(Activities.dropEverything())
                .build();
    }

    public static Activity masterGardner() {
        Position draynor = new Position(3079, 3256);

        Set<String> goodSeeds = new HashSet<>(Arrays.asList("Snapdragon seed", "Ranarr seed"));
        Predicate<Item> badSeed = item -> !goodSeeds.contains(item.getName()) && item.getName().contains("seed");

        Activity healIfNeeded = Activity.newBuilder()
                .withName("Eating food")
                .addPreReq(() -> Health.getCurrent() < 20)
                .addPreReq(() -> Inventory.contains("Shark"))
                .addSubActivity(() -> Inventory.getFirst("Shark").interact("Eat"))
                .build();

        Activity getSharksIfNeeded = Activity.newBuilder()
                .withName("Getting sharks")
                .addPreReq(() -> !Inventory.contains("Shark"))
                .addSubActivity(Activities.goToBank())
                .addSubActivity(Activities.openBank())
                .addSubActivity(Activities.withdraw("Shark", 26))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.moveTo(draynor))
                .build();

        return Activity.newBuilder()
                .withName("Pickpocketing Master Farmer")
                .addPreReq(() -> Npcs.getNearest("Master Farmer") != null)
                .addPreReq(() -> Health.getCurrent() > 10)
                .addPreReq(() -> !Inventory.isFull())
                .addSubActivity(getSharksIfNeeded)
                .addSubActivity(healIfNeeded)
                .addSubActivity(() -> Npcs.getNearest("Master Farmer").interact("Pickpocket"))
                .addSubActivity(Activities.pauseFor(Duration.ofSeconds(2)))
                .addSubActivity(Activities.sleepWhile(() -> Players.getLocal().isAnimating()))
                .addSubActivity(Activities.dropAll(badSeed))
                .build();
    }
}
