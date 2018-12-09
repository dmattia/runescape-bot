package favor;

import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.event.listeners.SkillListener;
import org.rspeer.runetek.event.types.SkillEvent;
import util.Activities;
import util.common.Activity;

import java.util.concurrent.atomic.AtomicBoolean;

public class Hosidius {
    public static Activity fertilizer() {
        return Activity.newBuilder()
                .addSubActivity(Activities.withdrawEqualOf("Compost", "Saltpetre"))
                .addSubActivity(Activities.use("Compost", "Saltpetre"))
                .addSubActivity(waitForProductionToStop("Compost"))
                .onlyOnce()
                .build();
    }

    // TODO(dmattia): Refactor with crafting method by same name
    private static Activity waitForProductionToStop(String inputName) {
        return Activity.newBuilder()
                .addSubActivity(() -> {
                    AtomicBoolean complete = new AtomicBoolean(false);
                    SkillListener listener = event -> {
                        if (event.getType() == SkillEvent.TYPE_LEVEL && event.getSource() == Skill.FARMING) {
                            complete.set(true);
                        }
                    };

                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> complete.get() || !Inventory.contains(inputName), 1000 * 60);
                    Game.getEventDispatcher().deregister(listener);
                })
                .onlyOnce()
                .build();
    }

    /**
     * Used while favor < 5% until you can mix componst with saltpetre.
     * This could also be used well beyond 5% if desired.
     */
    /*
    public static Activity pushPlough() {
        Position northField = new Position(1765, 3535);
        Position northEdge = new Position(1765, 3539);
        Position southField = new Position(1765, 3525);
        Position southEdge = new Position(1765, 3521);

        return Activity.newBuilder()
                .addSubActivity(Activities.moveTo(southEdge))
                .addSubActivity(
                        Activity.newBuilder()
                                .addPreReq(() -> Players.getLocal().distance(northField) > 0.5)
                                .addSubActivity(() -> Npcs.getNearest(6924).click())
                                .addSubActivity(() -> {
                                    Time.sleepUntil(() -> {
                                        Npc plough = Npcs.getNearest(6924);

                                        if (plough.containsAction("Repair")) return true;

                                        return Players.getLocal().distance(northField) < 1;
                                    }, 1000 * 30);
                                })
                                .untilPreconditionsFail()
                                .build()
                )
                .addSubActivity(Activities.moveTo(northEdge))
                .addSubActivity(
                        Activity.newBuilder()
                                .addPreReq(() -> Players.getLocal().distance(southField) > 0.5)
                                .addSubActivity(() -> Npcs.getNearest(6924).click())
                                .addSubActivity(() -> {
                                    Time.sleepUntil(() -> {
                                        Npc plough = Npcs.getNearest(6924);

                                        if (plough.containsAction("Repair")) return true;

                                        return Players.getLocal().distance(southField) < 1;
                                    }, 1000 * 30);
                                })
                                .untilPreconditionsFail()
                                .build()
                )
                .build();
    }
    */
}
