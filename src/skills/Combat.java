package skills;

import misc.zulrah.*;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.Varps;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.component.tab.Prayer;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.InstancePosition;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.movement.transportation.FairyRing;
import org.rspeer.runetek.api.scene.House;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.DeathListener;
import org.rspeer.runetek.event.listeners.RenderListener;
import sun.jvm.hotspot.oops.Instance;
import util.Activities;
import util.Globals;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Activities related to Combat.
 */
public class Combat {
    // TODO(dmattia): Refactor, moving to Activities, and making it not suck.
    private static Activity moveInDirection(int x, int y) {
        return Activity.newBuilder()
                .addSubActivity(() -> {
                    Position pos = Players.getLocal().getPosition();
                    while(pos.isPositionWalkable()) {
                        pos = pos.translate(x, y);
                    }
                    pos = pos.translate(-x, -y);
                    while(!Movement.setWalkFlagWithConfirm(pos)) {
                        pos = pos.translate(-x, -y);
                    }
                    Time.sleepUntil(() -> !Movement.isDestinationSet(), 1000 * 10);
                })
                .build();
    }

    /**
     * Runs to the north and kills stuff. If you die, it just goes back and kills more stuff (the minigame is completely
     * safe).
     */
    public static Activity pestControl() {
        Position veteranBoat = new Position(2638, 2653);
        BooleanSupplier isOnIsland = Game.getClient()::isInInstancedScene;

        Function<Position, Activity> moveTo = position -> Activity.newBuilder()
                    .withName("Moving to position: " + position)
                    .addPreReq(() -> Movement.isWalkable(position))
                    .addPreReq(() -> position.distance(Players.getLocal()) > 5)
                    .addSubActivity(Activities.toggleRun())
                    .addSubActivity(() -> Movement.walkTo(position))
                    .thenPauseFor(Duration.ofSeconds(4))
                    .maximumDuration(Duration.ofSeconds(20))
                    .untilPreconditionsFail()
                    .build();

        Activity boardShip = Activity.newBuilder()
                .withName("Boarding ship to start game")
                .addPreReq(() -> SceneObjects.getNearest("Gangplank") != null)
                .addPreReq(() -> !isOnIsland.getAsBoolean())
                .addPreReq(() -> Players.getLocal().getX() >= 2638)
                .tick()
                .addSubActivity(Activities.moveTo(veteranBoat))
                .addSubActivity(() -> SceneObjects.getNearest("Gangplank").interact("Cross"))
                .thenPauseFor(Duration.ofSeconds(3))
                .build();

        Activity waitForGameToStart = Activity.newBuilder()
                .withName("Waiting for game to start")
                .addPreReq(() -> !isOnIsland.getAsBoolean())
                .addPreReq(() -> Players.getLocal().getX() < 2638)
                .addSubActivity(() -> Time.sleepUntil(isOnIsland, 1000 * 60 * 10))
                .build();

        Activity goToSpawnSpot = Activity.newBuilder()
                .withName("Going to my fav spot")
                .addPreReq(isOnIsland)
                .addSubActivity(() -> moveTo.apply(Players.getLocal().getPosition().translate(0, -31)).run())
                .build();

        Activity killStuff = Activity.newBuilder()
                .withName("killing stuff")
                .addPreReq(isOnIsland)
                .addPreReq(() -> Npcs.getNearest(npc -> true) != null)
                .addPreReq(() -> Npcs.getNearest(npc -> true).containsAction("Attack"))
                .addSubActivity(() -> Npcs.getNearest(npc -> true).interact("Attack"))
                .thenSleepUntil(() -> Players.getLocal().getTarget() == null)
                .build();

        return Activity.newBuilder()
                // TODO(dmattia): Check for world 344
                .withName("Pest Control")
                .addSubActivity(boardShip)
                .addSubActivity(waitForGameToStart)
                .addSubActivity(goToSpawnSpot)
                .addSubActivity(killStuff)
                .build();
    }

    public static Activity zulrah() {
        // During each phase, find zulrah, classify using phase classifier (to get phase history to this point).
        // Then check the history against the patterns, finding a PatternPhase.
        // A PatternPhase should have a a ZulrahState and a Position.
        // Once the PatternPhase is found, run an activity that:
        //   - Prays for the current PatternPhase.state.style
        //   - Moves Exactly to the PatternPhase.playerPosition
        //   - Eat food if < 55 hp
        //   - Attack Zulrah

        // Every PatternPhase should have it's own Activity, so that things like "Move after the first mage animation
        // finishes" and "Jad it up" are possible. There should be a default activity that just prays, moves to a
        // position, and attacks profit snek.

        // AGAINST RED MELEE FORM: Use mage, no protect prayer
        // AGAINST GREEN RANGE FORM: Use mage, protect range
        // AGAINST BLUE MAGE FORM: Use range, protect mage
        // AGAINST JAD FORM: Use mage, alternate protects. Pray opposite of whatever projectile you can see

        // Turn off auto-retaliate

        Activity turnOffAllPrayers = Arrays.stream(Prayers.getActive())
                .map(prayer -> Activities.unpray(prayer))
                .collect(new ActivityCollector())
                .addPreReq(House::isInside)
                .withName("Turning off all prayers")
                .build();

        Activity drinkFromPool = Activity.newBuilder()
                .withName("Drinking from pool for anti-venom + health + prayer")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Ornate rejuvenation pool") != null)
                .tick()
                .addSubActivity(() -> SceneObjects.getNearest("Ornate rejuvenation pool").click())
                .thenSleepUntil(() -> Movement.getRunEnergy() == 100)
                .withoutPausingBetweenActivities()
                .build();

        Activity stepStone = Activity.newBuilder()
                .onlyIfInArea(Area.rectangular(2145, 3075, 2154, 3066))
                .addPreReq(() -> SceneObjects.getNearest("Stepping stone") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Stepping stone").interact("Cross"))
                .completeAnimation()
                .build();

        Activity goToDock = Activity.newBuilder()
                .onlyIfInArea(Area.rectangular(2158, 3076, 2207, 3045))
                .addSubActivity(Activities.moveTo(new Position(2211, 3056)))
                .build();

        Activity prepareMage = Activity.newBuilder()
                .withName("Preparing for mage form")
                .addPreReq(() -> ZulrahStyle.classify().equals(ZulrahStyle.MAGE))
                .addPreReq(() -> !Equipment.contains("Trident of the swamp"))
                .addSubActivity(Activities.pray(Prayer.PROTECT_FROM_MAGIC))
                .addSubActivity(Activities.unpray(Prayer.AUGURY))
                .addSubActivity(Activities.pray(Prayer.EAGLE_EYE))
                .addSubActivity(Activities.equip("Pegasian boots"))
                .addSubActivity(Activities.equip("Toxic blowpipe"))
                .addSubActivity(Activities.equip("Necklace of anguish"))
                .addSubActivity(Activities.equip("Void ranger helm"))
                .addSubActivity(Activities.equip("Ava's accumulator"))
                .withoutPausingBetweenActivities()
                .build();

        Activity prepareRange = Activity.newBuilder()
                .withName("Preparing for range form")
                .addPreReq(() -> ZulrahStyle.classify().equals(ZulrahStyle.RANGED))
                .addPreReq(() -> !Equipment.contains("Toxic blowpipe"))
                .addSubActivity(Activities.pray(Prayer.PROTECT_FROM_MISSILES))
                .addSubActivity(Activities.unpray(Prayer.EAGLE_EYE))
                .addSubActivity(Activities.pray(Prayer.AUGURY))
                .addSubActivity(Activities.equip("Eternal boots"))
                .addSubActivity(Activities.equip("Void mage helm"))
                .addSubActivity(Activities.equip("Occult necklace"))
                .addSubActivity(Activities.equip("Trident of the swamp"))
                .addSubActivity(Activities.equip("Mage's book"))
                .addSubActivity(Activities.equip("Imbued saradomin cape"))
                .withoutPausingBetweenActivities()
                .build();

        Activity prepareMelee = Activity.newBuilder()
                .withName("Preparing for melee form")
                .addPreReq(() -> ZulrahStyle.classify().equals(ZulrahStyle.MELEE))
                .addPreReq(() -> !Equipment.contains("Trident of the swamp"))
                .addSubActivity(Activities.turnOffAllPrayers())
                .addSubActivity(Activities.pray(Prayer.AUGURY))
                .addSubActivity(Activities.equip("Eternal boots"))
                .addSubActivity(Activities.equip("Void mage helm"))
                .addSubActivity(Activities.equip("Occult necklace"))
                .addSubActivity(Activities.equip("Trident of the swamp"))
                .addSubActivity(Activities.equip("Mage's book"))
                .addSubActivity(Activities.equip("Imbued saradomin cape"))
                .withoutPausingBetweenActivities()
                .build();

        Activity takePhaseAction = Activity.newBuilder()
                .addSubActivity(() -> PatternClassifier
                        .classifyPattern()
                        .map(PatternClassifier.PatternPhase::getActivity)
                        .ifPresent(Activity::run))
                .build();

        Activity killZulrah = Activity.newBuilder()
                .withName("Handling zulrah instance")
                .addPreReq(Game.getClient()::isInInstancedScene)
                .addPreReq(() -> !House.isInside())
                .addSubActivity(prepareMage)
                .addSubActivity(prepareMelee)
                .addSubActivity(prepareRange)
                .addSubActivity(takePhaseAction)
                .thenPauseFor(Duration.ofMillis(600))
                .withoutPausingBetweenActivities()
                .untilPreconditionsFail()
                .build()
                .addPreActivity(Activity.of(() -> PhaseClassifier.reset()));

        return Activity.newBuilder()
                .addSubActivity(turnOffAllPrayers)
                .addSubActivity(drinkFromPool)
                .addSubActivity(Activities.useHouseFairyRing(FairyRing.Destination.BJS))
                .addSubActivity(stepStone)
                .addSubActivity(goToDock)
                .addSubActivity(killZulrah)
                .withoutPausingBetweenActivities()
                .build();
    }

    public static Activity nmz() {
        Position cofferArea = new Position(2609, 3115);
        int ABSORPTION_MIN = 100;

        Activity moveNorthEast = Activity.newBuilder()
                .withName("Moving to northeast corner")
                .addSubActivity(moveInDirection(1, 1))
                .addSubActivity(moveInDirection(1, 0))
                .addSubActivity(moveInDirection(0, 1))
                .build();

        Supplier<Activity> overload = () -> Activity.newBuilder()
                .withName("Drinking overload")
                .addPreReq(() -> Inventory.contains(item -> item.getName().contains("Overload")))
                .addPreReq(() -> Health.getCurrent() > 50)
                .addPreReq(() -> Varps.getBitValue(3955) <= 1)
                .addSubActivity(() -> {
                    int currentHealth = Health.getCurrent();
                    Inventory.getFirst(item -> item.getName().contains("Overload")).click();
                    Time.sleepUntil(() -> Health.getCurrent() <= currentHealth - 50, 1000 * 10);
                })
                .build();

        Supplier<Activity> eatRockCake = () -> Activity.newBuilder()
                .withName("Eating rock cake to lower health")
                .addSubActivity(() -> Inventory.contains("Dwarven rock cake"))
                .addPreReq(() -> Health.getCurrent() > 1)
                .addSubActivity(() -> Inventory.getFirst("Dwarven rock cake").interact("Guzzle"))
                .thenPauseFor(Duration.ofSeconds(1))
                .untilPreconditionsFail()
                .build();

        Activity prepareForBattle = Activity.newBuilder()
                .withName("Preparing for battle")
                .addPreReq(() -> Prayers.getPoints() > 0)
                .addSubActivity(Activities.switchToTab(Tab.PRAYER))
                .addSubActivity(() -> Prayers.toggle(true, Prayer.PROTECT_FROM_MELEE))
                .addSubActivity(moveNorthEast)
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .build();

        return Activity.newBuilder()
                .withName("Handling inside of NMZ")
                .addPreReq(() -> Inventory.contains(item -> item.getName().contains("Overload")))
                .addPreReq(() -> Inventory.contains(item -> item.getName().contains("Absorption")))
                .addPreReq(() -> Inventory.contains("Dwarven rock cake"))
                .addPreReq(() -> Players.getLocal().distance(cofferArea) > 100)
                .addSubActivity(prepareForBattle)
                .addSubActivity(() -> {
                    AtomicBoolean hasDied = new AtomicBoolean(false);
                    DeathListener deathListener = event -> {
                      if (event.getSource() == Players.getLocal()) {
                          hasDied.set(true);
                      }
                    };

                    Game.getEventDispatcher().register(deathListener);

                    Activity absorption = Activity.newBuilder()
                            .withName("Drinking Absorption")
                            .addPreReq(() -> Inventory.contains(item -> item.getName().contains("Absorption")))
                            .addPreReq(() -> Varps.getBitValue(3956) < ABSORPTION_MIN)
                            .addPreReq(() -> Inventory.getFirst(item -> item.getName().contains("Absorption")).click())
                            .build();

                    Activity useSpecialAttack = Activity.newBuilder()
                            .withName("Using special attack")
                            .addPreReq(() -> Players.getLocal().getTarget() != null)
                            .addPreReq(() -> org.rspeer.runetek.api.component.tab.Combat.getSpecialEnergy() == 100)
                            .addPreReq(() -> !org.rspeer.runetek.api.component.tab.Combat.isSpecialActive())
                            .addSubActivity(Activities.switchToTab(Tab.COMBAT))
                            .addSubActivity(() -> org.rspeer.runetek.api.component.tab.Combat.toggleSpecial(true))
                            .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                            .build();

                    Activity.newBuilder()
                            .addPreReq(() -> !hasDied.get())
                            .addPreReq(() -> Players.getLocal().distance(cofferArea) > 100)
                            .addSubActivity(overload.get())
                            .addSubActivity(absorption)
                            .addSubActivity(eatRockCake.get())
                            .addSubActivity(useSpecialAttack)
                            .thenPauseFor(Duration.ofMillis(450))
                            .untilPreconditionsFail()
                            .build()
                            .run();

                    Game.getEventDispatcher().deregister(deathListener);
                })
                .build();
    }

    public static Activity kill(Enemy enemy) {
        return Activity.newBuilder()
                .withName("Killing " + enemy.getName())
                .addPreReq(() -> Health.getPercent() > 20)
                .addPreReq(() -> Prayers.getPoints() > 0)
                .addSubActivity(Activities.moveTo(enemy.getPosition()))
                .addSubActivity(Combat.kill(enemy.getName()))
                .build();
    }

    public static Activity attack(String npcName) {
        return Activity.newBuilder()
                .withName("Attacking " + npcName)
                .addPreReq(() -> Npcs.getNearest(npcName) != null)
                .addPreReq(() -> Npcs.getNearest(npcName).getTarget() == null)
                .addPreReq(() -> Players.getLocal().getTarget() == null)
                .addSubActivity(() -> Npcs.getNearest(npcName).interact("Attack"))
                .build();
    }

    public static Activity waitUntilTargetKilled() {
        return Activity.newBuilder()
                .withName("Waiting until target is killed")
                .addPreReq(() -> Players.getLocal().getTarget() != null)
                .addSubActivity(() -> {
                    AtomicBoolean enemyKilled = new AtomicBoolean(false);
                    DeathListener deathListener = event -> enemyKilled.set(event.getSource() == Players.getLocal().getTarget());
                    Game.getEventDispatcher().register(deathListener);
                    Time.sleepUntil(() -> enemyKilled.get(), 600, 1000 * 60 * 3);
                    Game.getEventDispatcher().deregister(deathListener);
                })
                .onlyOnce()
                .build();
    }

    public static Activity hillGiants() {
        return Activity.newBuilder()
                .withName("Killing hill giants and burying their bones")
                .addSubActivity(Combat.kill("Hill Giant"))
                .addSubActivity(Activities.pickupAll("Big bones", "Cosmic rune", "Nature rune", "Law rune", "Death rune"))
                .addSubActivity(Activities.consumeAll("Big bones"))
                .onlyOnce()
                .build();
    }

    /**
     * Kills a single nearby enemy by name. If no enemy is nearby, no action is taken.
     *
     * @param npcName The name of the enemy to attack
     */
    public static Activity kill(String npcName) {

        return Activity.newBuilder()
                .withName("Killing " + npcName)
                .addSubActivity(attack(npcName))
                .addSubActivity(waitUntilTargetKilled())
                .thenPauseFor(Duration.ofMillis(1500))
                .onlyOnce()
                .build();
    }

    public enum Enemy {
        GOBLIN("Goblin", new Position(3251, 3229));

        private String name;
        private Position position;

        Enemy(String name, Position position) {
            this.name = name;
            this.position = position;
        }

        public Position getPosition() {
            return position;
        }

        public String getName() {
            return name;
        }
    }
}
