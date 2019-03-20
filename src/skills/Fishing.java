package skills;

import misc.Kitten;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.Optional;

public class Fishing {
    public static Activity fishLureSpot() {
        return Activity.newBuilder()
                .withName("Fishing for trout")
                .addPreReq(() -> Inventory.contains("Feather"))
                .addPreReq(() -> Inventory.contains("Fly fishing rod"))
                .addPreReq(() -> Npcs.getNearest("Rod Fishing spot") != null)
                .addSubActivity(() -> {
                    Optional.ofNullable(Npcs.getNearest("Rod Fishing spot"))
                            .ifPresent(spot -> {
                                Activity.newBuilder()
                                        .addSubActivity(Activities.moveTo(spot.getPosition()))
                                        .addSubActivity(() -> spot.interact("Lure"))
                                        .build()
                                        .run();
                            });
                })
                .thenPauseFor(Duration.ofSeconds(5))
                .addSubActivity(() -> Time.sleepWhile(() -> Players.getLocal().getAnimation() != -1, 1876, 1000 * 60 * 5))
                .addSubActivity(Kitten.pet())
                .addSubActivity(Kitten.feed("Raw salmon"))
                .addSubActivity(Activities.dropAll("Raw trout"))
                .addSubActivity(Activities.dropAll("Raw salmon"))
                .build();
    }
}
