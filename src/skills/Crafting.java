package skills;

import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.SkillListener;
import org.rspeer.runetek.event.types.SkillEvent;
import util.Activities;
import util.common.Activity;

import java.util.concurrent.atomic.AtomicBoolean;

public class Crafting {
    private static final Position EDGEVILLE_FURNACE = new Position(3107, 3498);

    //TODO(dmattia): refactor
    public static Activity makeJewlery(Jewlery type) {
        Activity fetchMaterials = Activity.newBuilder()
                .withName("Fetching materials")
                .addPreReq(() -> !Inventory.contains(type.getBarName()))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.withdraw(type.getMouldName(), 1))
                .addSubActivity(Activities.withdraw(type.getBarName(), 13))
                .addSubActivity(Activities.withdraw(type.getGemName(), 13))
                .addSubActivity(Activities.closeBank())
                .build();

        return Activity.newBuilder()
                .withName("Making " + type.getOutputName())
                .addSubActivity(fetchMaterials)
                .addSubActivity(Activities.moveTo(EDGEVILLE_FURNACE))
                .addSubActivity(Activities.use(type.getBarName()))
                .addSubActivity(() -> SceneObjects.getNearest("Furnace").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(() -> Interfaces.getFirst(component -> component.getName().contains(type.getOutputName())) != null, 1000 * 5))
                .addSubActivity(() -> Interfaces.getFirst(component -> component.getName().contains(type.getOutputName())).interact(type.getAction()))
                .addSubActivity(waitForProductionToStop(type.getBarName()))
                .build();
    }

    public static Activity goldBracelets() {
        return Activity.newBuilder()
                .withName("Gold Bracelets")

                .addSubActivity(getGoldBars())
                .addSubActivity(Activities.moveTo(EDGEVILLE_FURNACE))
                .addSubActivity(Activities.use("Gold bar"))
                .addSubActivity(() -> SceneObjects.getNearest("Furnace").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(() -> Interfaces.getFirst(component -> component.getName().contains("Gold bracelet")) != null, 1000 * 5))

                .addSubActivity(() -> Interfaces.getFirst(component -> component.getName().contains("Gold bracelet")).interact("Make-All"))
                .addSubActivity(waitForProductionToStop("Gold bar"))

                .build();
    }

    private static Activity getGoldBars() {
        return Activity.newBuilder()
                .withName("Getting gold bars")
                .addPreReq(() -> !Inventory.contains("Gold bar"))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.withdraw("Bracelet mould", 1))
                .addSubActivity(Activities.withdraw("Gold bar", 27))
                .addSubActivity(Activities.closeBank())
                .build();
    }

    private static Activity waitForProductionToStop(String itemProduced) {
        return Activity.newBuilder()
                .withName("Producing " + itemProduced + " until complete or next level")
                .addSubActivity(() -> {
                    AtomicBoolean complete = new AtomicBoolean(false);
                    SkillListener listener = event -> {
                        if (event.getType() == SkillEvent.TYPE_LEVEL && event.getSource() == Skill.CRAFTING) {
                            complete.set(true);
                        }
                    };

                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> complete.get() || !Inventory.contains(itemProduced), 1000 * 60);
                    Game.getEventDispatcher().deregister(listener);
                })
                .onlyOnce()
                .build();
    }

    public enum Jewlery {
        RUBY_BRACELET("Gold bar", "Ruby", "Bracelet moult", "Ruby bracelet", "Make-all"),
        TOPAZ_AMULET("Silver bar", "Red topaz", "Amulet mould", "Topaz amulet (u)", "Craft all");

        private String barName;
        private String gemName;
        private String mouldName;
        private String outputName;
        private String action;

        Jewlery(String barName, String gemName, String mouldName, String outputName, String action) {
            this.barName = barName;
            this.gemName = gemName;
            this.mouldName = mouldName;
            this.outputName = outputName;
            this.action = action;
        }

        public String getOutputName() {
            return outputName;
        }

        public String getMouldName() {
            return mouldName;
        }

        public String getBarName() {
            return barName;
        }

        public String getGemName() {
            return gemName;
        }

        public String getAction() {
            return action;
        }
    }
}
