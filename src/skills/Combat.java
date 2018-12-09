package skills;

import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.event.listeners.DeathListener;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activities related to Combat.
 */
public class Combat {
    public static Activity kill(Enemy enemy) {
        return Activity.newBuilder()
                .addSubActivity(Activities.moveTo(enemy.getPosition()))
                .addSubActivity(Combat.kill(enemy.getName()))
                .build();
    }

    public static Activity attack(String npcName) {
        Npc enemy = Npcs.getNearest(npc -> npc.getName().equals(npcName) && npc.getTarget() == null);

        return Activity.newBuilder()
                .addPreReq(() -> enemy != null)
                .addSubActivity(() -> enemy.interact("Attack"))
                .onlyOnce()
                .build();
    }

    public static Activity waitUntilTargetKilled() {
        return Activity.newBuilder()
                .addPreReq(() -> Players.getLocal().getTarget() != null)
                .addSubActivity(() -> {
                    AtomicBoolean enemyKilled = new AtomicBoolean(false);
                    DeathListener listener = event -> enemyKilled.set(event.getSource() == Players.getLocal().getTarget());

                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> enemyKilled.get(), 1000, 1000 * 60 * 3);
                    Game.getEventDispatcher().deregister(listener);
                })
                .onlyOnce()
                .build();
    }

    public static Activity hillGiants() {
        return Activity.newBuilder()
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
