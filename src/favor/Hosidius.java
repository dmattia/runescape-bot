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
}
