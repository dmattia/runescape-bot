package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.Optional;

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
}
