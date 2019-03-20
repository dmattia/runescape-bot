package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.movement.transportation.FairyRing;
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
                .tick()
                .addSubActivity(() -> Dialog.process(0))
                .thenPauseFor(Duration.ofMillis(369))

                .addSubActivity(Activities.use(NOTED_MARRENTILL))
                .addSubActivity(() -> Npcs.getNearest("Phials").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000 * 3))
                .tick()
                .addSubActivity(() -> Dialog.process(0))
                .thenPauseFor(Duration.ofMillis(369))

                .addSubActivity(Activities.use(NOTED_BONES))
                .addSubActivity(() -> Npcs.getNearest("Phials").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000 * 3))
                .tick()
                .addSubActivity(() -> Dialog.process(2))
                .tick()
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
                                .thenPauseFor(Duration.ofMillis(567))
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

    public static Activity ensouledHeads() {
        Activity getRing = Activity.newBuilder()
                .withName("Getting new ring of dueling")
                .addPreReq(() -> !EquipmentSlot.RING.getItemName().contains("Ring of dueling"))
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Ring of dueling(8)", 1))
                .build();

        Activity prepareBank = Activity.newBuilder()
                .withName("Preparing bank")
                .onlyIfInArea(Area.rectangular(2433, 3099, 2448, 3079))
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> !Inventory.contains("Ensouled giant head"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(getRing)
                .addSubActivity(Activities.withdrawAll("Ensouled giant head"))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(Activities.equip("Ring of dueling(8)"))
                .addSubActivity(Activities.equip("Dramen staff"))
                .build();

        Activity goHome = Activity.newBuilder()
                .withName("Going home")
                .onlyIfInArea(Area.rectangular(2433, 3099, 2448, 3079))
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> Inventory.contains("Ensouled giant head"))
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").click())
                .thenSleepUntil(House::isInside)
                .tick()
                .build();

        Activity drinkFromPool = Activity.newBuilder()
                .withName("Drinking from pool for some prayer restoration")
                .addPreReq(House::isInside)
                .addPreReq(() -> Inventory.contains("Ensouled giant head"))
                .addPreReq(() -> SceneObjects.getNearest("Ornate rejuvenation pool") != null)
                .addPreReq(() -> Prayers.getPoints() < Skills.getLevel(Skill.PRAYER))
                .addSubActivity(() -> SceneObjects.getNearest("Ornate rejuvenation pool").click())
                .thenSleepUntil(() -> Prayers.getPoints() == Skills.getLevel(Skill.PRAYER))
                .withoutPausingBetweenActivities()
                .build();

        Activity goNearDarkAltar = Activity.newBuilder()
                .withName("Going near dark altar")
                .onlyIfInArea(Area.rectangular(1634, 3873, 1645, 3863))
                .addSubActivity(Activities.moveTo(new Position(1715, 3879)))
                .build();

        Activity goBackInArea = Activity.newBuilder()
                .addPreReq(Dialog::isOpen)
                .addSubActivity(Activities.moveTo(new Position(1715, 3879)))
                .build();

        Activity killHeads = Activity.newBuilder()
                .withName("Killing ensouled heads")
                .addPreReq(() -> Inventory.contains("Ensouled giant head"))
                .addPreReq(() -> Magic.canCast(Spell.Necromancy.REANIMATE_GIANT))
                .addPreReq(() -> Health.getCurrent() > 25)
                .onlyIfInArea(Area.rectangular(1660, 3903, 1733, 3857))
                .addSubActivity(Activities.equip("Abyssal whip"))
                .addSubActivity(Activities.pray(org.rspeer.runetek.api.component.tab.Prayer.ULTIMATE_STRENGTH))
                .addSubActivity(goBackInArea)
                .addSubActivity(Mage.castOn(Spell.Necromancy.REANIMATE_GIANT, "Ensouled giant head"))
                .thenPauseFor(Duration.ofSeconds(8))
                .addSubActivity(Combat.attack("Reanimated giant"))
                .thenSleepUntil(() -> Players.getLocal().getTarget() == null)
                .untilPreconditionsFail()
                .build();

        Activity goToBank = Activity.newBuilder()
                .withName("Heading to castle wars to bank")
                .onlyIfInArea(Area.rectangular(1660, 3903, 1733, 3857))
                .addPreReq(() -> !Inventory.contains("Ensouled giant head") || Health.getCurrent() < 26)
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Castle Wars"))
                .thenSleepUntil(() -> SceneObjects.getNearest("Bank chest") != null)
                .tick()
                .build();

        return Activity.newBuilder()
                .addPreReq(() -> Inventory.contains("Dramen staff") || Equipment.contains("Dramen staff"))
                .addPreReq(() -> Inventory.contains("Abyssal whip") || Equipment.contains("Abyssal whip"))
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                .addPreReq(() -> Magic.getSpellBook() == Magic.SPELLBOOK_NECROMANCY)
                .addPreReq(() -> org.rspeer.runetek.api.component.tab.Combat.isAutoRetaliateOn())
                .addSubActivity(prepareBank)
                .addSubActivity(goHome)
                .addSubActivity(drinkFromPool)
                .addSubActivity(Activities.useHouseFairyRing(FairyRing.Destination.CIS))
                .addSubActivity(goNearDarkAltar)
                .addSubActivity(killHeads)
                .addSubActivity(goToBank)
                .build();
    }
}
