package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.*;
import org.rspeer.runetek.event.listeners.SkillListener;
import org.rspeer.runetek.event.types.SkillEvent;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class Prayer {
    private static final Position PHIALS = new Position(2950, 3213);
    private static final Predicate<Item> UNNOTED_BONES = item -> !item.isNoted() && item.getName().contains("bones");
    private static final Predicate<Item> NOTED_BONES = item -> item.isNoted() && item.getName().contains("bones");
    private static final Predicate<Item> UNNOTED_MARRENTILL = item ->
            !item.isNoted() && item.getName().equals("Marrentill");
    private static final Predicate<Item> NOTED_MARRENTILL = item ->
            item.isNoted() && item.getName().equals("Marrentill");



    public static Activity getCalcium() {
        Activity unnoteItems = Activity.newBuilder()
                .withName("Unnoting marrentill and bones")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> Players.getLocal().distance(PHIALS) < 100)
                .addPreReq(() -> !Inventory.isFull())
                .addSubActivity(Activities.moveTo(PHIALS))

                // TODO(dmattia): dedup this, made a bit annoying by the options Phials gives.
                .addSubActivity(Activities.use(NOTED_MARRENTILL))
                .addSubActivity(() -> Npcs.getNearest("Phials").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000 * 3))
                .addSubActivity(() -> Activities.pauseFor(Duration.ofSeconds(1)))
                .addSubActivity(() -> Dialog.process(0))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(369)))

                .addSubActivity(Activities.use(NOTED_MARRENTILL))
                .addSubActivity(() -> Npcs.getNearest("Phials").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000 * 3))
                .addSubActivity(() -> Activities.pauseFor(Duration.ofSeconds(1)))
                .addSubActivity(() -> Dialog.process(0))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(369)))

                .addSubActivity(Activities.use(NOTED_BONES))
                .addSubActivity(() -> Npcs.getNearest("Phials").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000 * 3))
                .addSubActivity(() -> Activities.pauseFor(Duration.ofSeconds(1)))
                .addSubActivity(() -> Dialog.process(2))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(421)))
                .build();

        Activity enterHouse = Activity.newBuilder()
                .withName("Entering house")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> SceneObjects.getNearest("Portal") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Home"))
                .addSubActivity(() -> Time.sleepUntil(House::isInside, 1211, 1000 * 10))
                .untilPreconditionsFail()
                .build();

        Activity leaveHouse = Activity.newBuilder()
                .withName("Leaving house")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Portal") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Enter"))
                .addSubActivity(() -> Time.sleepWhile(House::isInside, 1211, 1000 * 10))
                .build();

        Activity lightBurners = Activity.newBuilder()
                .withName("Lighting burners")
                .addPreReq(House::isInside)
                .addPreReq(() -> Inventory.getCount(UNNOTED_MARRENTILL) >= 2)
                .addPreReq(() -> SceneObjects.getNearest("Incense burner") != null)
                .addSubActivity(() -> {
                    for (SceneObject burner : SceneObjects.getLoaded(so -> so.getName().contains("Incense burner"))) {
                        Activity.newBuilder()
                                .addSubActivity(Activities.moveTo(burner.getPosition()))
                                .addSubActivity(() -> burner.interact("light"))
                                .addSubActivity(Activities.pauseFor(Duration.ofMillis(567)))
                                .build()
                                .run();
                    }
                })
                .onlyOnce()
                .build();

        Activity offerBones = Activity.newBuilder()
                .withName("Offering bones")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addPreReq(() -> Inventory.contains(UNNOTED_BONES))
                .addSubActivity(() -> Activities.moveTo(SceneObjects.getNearest("Altar").getPosition()).run())
                .addSubActivity(Activities.use(UNNOTED_BONES))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> {
                    AtomicBoolean complete = new AtomicBoolean(false);
                    SkillListener listener = event -> {
                        if (event.getType() == SkillEvent.TYPE_LEVEL && event.getSource() == Skill.PRAYER) {
                            complete.set(true);
                        }
                    };

                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> complete.get() || !Inventory.contains(UNNOTED_BONES), 1000 * 60);
                    Game.getEventDispatcher().deregister(listener);
                })
                .untilPreconditionsFail()
                .build();

        return Activity.newBuilder()
                .withName("Offer dem bones")
                .addPreReq(() -> Inventory.getCount(true, "Coins") > 120) // Unnoting fee for Phials
                .addPreReq(() -> Inventory.getCount(true, "Marrentill") > 1)
                .addPreReq(() -> Inventory.getCount(true, item -> item.getName().contains("bones")) > 1)
                .addSubActivity(lightBurners)
                .addSubActivity(offerBones)
                .addSubActivity(leaveHouse)
                .addSubActivity(unnoteItems)
                .addSubActivity(enterHouse)
                .build();
    }
}
