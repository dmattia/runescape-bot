package skills;

import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.SkillListener;
import org.rspeer.runetek.event.types.SkillEvent;
import util.Activities;
import util.common.Activity;

import java.util.concurrent.atomic.AtomicBoolean;

public class Cooking {
    // TODO(dmattia): have foodType + Location enum options
    public static Activity cookAtRogues(FoodType food) {
        return Activity.newBuilder()
                .addSubActivity(getRawFood(food))
                .addSubActivity(Activities.use(food.getPreCookedName()))
                .addSubActivity(() -> SceneObjects.getNearest("Fire").interact("Use"))
                .addSubActivity(Activities.produceAll(food.getProductionName()))
                .addSubActivity(waitForProductionToStop(food))
                .onlyOnce()
                .build();
    }

    private static Activity getRawFood(FoodType food) {
        return Activity.newBuilder()
                .addPreReq(() -> !Inventory.contains(food.getPreCookedName()))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.withdraw(food.getPreCookedName(), 28))
                .addSubActivity(Activities.closeBank())
                .onlyOnce()
                .build();
    }

    // TODO(dmattia): Refactor with crafting method by same name
    private static Activity waitForProductionToStop(FoodType food) {
        return Activity.newBuilder()
                .addSubActivity(() -> {
                    AtomicBoolean complete = new AtomicBoolean(false);
                    SkillListener listener = event -> {
                        if (event.getType() == SkillEvent.TYPE_LEVEL && event.getSource() == Skill.COOKING) {
                            complete.set(true);
                        }
                    };

                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> complete.get() || !Inventory.contains(food.getPreCookedName()), 1000 * 60);
                    Game.getEventDispatcher().deregister(listener);
                })
                .onlyOnce()
                .build();
    }

    public enum FoodType {
        TUNA("Raw tuna", "Tuna");

        private String preCookedName;
        private String productionName;

        FoodType(String preCookedName, String productionNamem) {
            this.preCookedName = preCookedName;
            this.productionName = productionName;
        }

        public String getPreCookedName() {
            return preCookedName;
        }

        public String getProductionName() {
            return productionName;
        }
    }
}
