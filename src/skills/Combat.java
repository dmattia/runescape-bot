package skills;

import org.rspeer.runetek.adapter.Varpbit;
import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Streams;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Prayer;
import org.rspeer.runetek.api.component.tab.Prayers;
import org.rspeer.runetek.api.component.tab.Tab;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.AnimationListener;
import org.rspeer.runetek.event.listeners.DeathListener;
import org.rspeer.runetek.event.listeners.VarpListener;
import org.rspeer.runetek.event.types.AnimationEvent;
import util.Activities;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.rspeer.runetek.event.types.AnimationEvent.TYPE_FINISHED;
import static org.rspeer.runetek.event.types.AnimationEvent.TYPE_STARTED;

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

    public static Activity nmz() {
        Activity moveNorthEast = Activity.newBuilder()
                .addSubActivity(moveInDirection(1, 1))
                .addSubActivity(moveInDirection(1, 0))
                .addSubActivity(moveInDirection(0, 1))
                .build();

        return Activity.newBuilder()
                .addSubActivity(Activities.switchToTab(Tab.PRAYER))
                .addSubActivity(() -> Prayers.toggle(true, Prayer.PROTECT_FROM_MELEE))
                .addSubActivity(moveNorthEast)
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> Inventory.getFirst(item -> item.getName().contains("Overload")).click())
                .addSubActivity(() -> Inventory.getFirst(item -> item.getName().contains("Absorption")).click())
                .addSubActivity(Activities.pauseFor(Duration.ofSeconds(10))) // Wait for overload to kick in
                .addSubActivity(() -> {
                    while(Health.getCurrent() > 1) {
                        Inventory.getFirst("Dwarven rock cake").interact("Guzzle");
                        Time.sleep(1100, 4200);
                    }
                })
                .addSubActivity(() -> {
                    ConcurrentLinkedQueue<Runnable> bufferedActions = new ConcurrentLinkedQueue();

                    VarpListener listener = event -> {
                        for (Varpbit varp : event.getChanges()) {
                            if (varp.getKey() == 3956) {
                                if (varp.getValue() <= 40) {
                                    bufferedActions.add(() -> {
                                        Time.sleep(500, 1200);
                                        Inventory.getFirst(item -> item.getName().contains("Absorption")).click();
                                    });
                                }
                            }

                            if (varp.getKey() == 3955) {
                                if (varp.getValue() == 1) {
                                    bufferedActions.add(() -> {
                                        Time.sleep(500, 1200);
                                        if (Inventory.contains(item -> item.getName().contains("Overload"))) {
                                            Inventory.getFirst(item -> item.getName().contains("Overload")).click();
                                        }
                                    });
                                }
                            }
                        }
                    };

                    Game.getEventDispatcher().register(listener);
                    while(Inventory.contains(item -> item.getName().contains("Absorption"))) {
                        if (Health.getCurrent() > 1 && Health.getCurrent() < 10) {
                            Inventory.getFirst("Dwarven rock cake").interact("Guzzle");
                        }
                        /*
                        bufferedActions.stream().map(Activity::of).collect(new ActivityCollector()).run();
                        bufferedActions.clear();
                        */
                        while(!bufferedActions.isEmpty()) {
                            bufferedActions.poll().run();
                            Time.sleep(300, 650);
                        }
                        Time.sleep(900, 2200);
                    }
                    Game.getEventDispatcher().deregister(listener);
                })
                .build();
    }

    public static Activity kill(Enemy enemy) {
        return Activity.newBuilder()
                .withName("Killing " + enemy.getName())
                .addSubActivity(Activities.moveTo(enemy.getPosition()))
                .addSubActivity(Combat.kill(enemy.getName()))
                .build();
    }

    public static Activity attack(String npcName) {
        Npc enemy = Npcs.getNearest(npc -> npc.getName().equals(npcName) && npc.getTarget() == null);

        return Activity.newBuilder()
                .withName("Attacking " + npcName)
                .addPreReq(() -> enemy != null)
                .addSubActivity(() -> enemy.interact("Attack"))
                .onlyOnce()
                .build();
    }

    public static Activity waitUntilTargetKilled() {
        return Activity.newBuilder()
                .withName("Waiting until target is killed")
                .addPreReq(() -> Players.getLocal().getTarget() != null)
                .addSubActivity(() -> {
                    AtomicBoolean enemyKilled = new AtomicBoolean(false);
                    DeathListener deathListener = event -> enemyKilled.set(event.getSource() == Players.getLocal().getTarget());
                    AnimationListener animationListener = event -> {
                      if (event.getType() == TYPE_FINISHED && event.getSource() == Players.getLocal().getTarget()) {
                          System.out.println("Enemy started animation");
                          //Prayers.flick(Prayer.PROTECT_FROM_MELEE, 60);
                          Prayers.toggle(true, Prayer.PROTECT_FROM_MELEE);
                          Time.sleep(60, 100);
                          Prayers.toggle(false, Prayer.PROTECT_FROM_MELEE);
                          System.out.println("Prayer turned off");
                      }
                      //System.out.println("Animation event recorded 2");
                    };

                    Game.getEventDispatcher().register(deathListener);
                    Game.getEventDispatcher().register(animationListener);
                    Time.sleepUntil(() -> enemyKilled.get(), 600, 1000 * 60 * 3);
                    Game.getEventDispatcher().deregister(deathListener);
                    Game.getEventDispatcher().deregister(animationListener);
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
                // TODO(dmattia): Wait for death animation, which is about 1500ms
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(1500)))
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
