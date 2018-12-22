package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Tab;
import org.rspeer.runetek.api.component.tab.Tabs;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;
import java.util.Arrays;

public class Mining {
    private static final short[] COPPER_COLORS = {4645};
    private static final short[] IRON_COLORS = {2576};
    private static final short[] GEM_COLORS = {-10335};

    /**
     * Mines gems at Shilo village, banking between inventories and hopping away from other players
     *
     * @return
     */
    public static Activity mineGems() {
        Position bankers = new Position(2850, 2955);
        Position rocks = new Position(2823, 2988);

        // Gems are busy even if one other person is there
        Activity hopIfBusy = Activity.newBuilder()
                .withName("Hopping because another player is around")
                .addPreReq(() -> Players.getLoaded(player -> {
                    return !player.equals(Players.getLocal()) && player.distance(Players.getLocal()) < 20;
                }).length > 0)
                .addPreReq(() -> Players.getLocal().distance(rocks) < 15)
                .addSubActivity(Activities.hopWorlds())
                .untilPreconditionsFail()
                .build();

        Activity mine = Activity.newBuilder()
                .withName("Mining gem rock")
                .addPreReq(() -> !Inventory.isFull())
                .addPreReq(() -> SceneObjects.getNearest(sceneObject -> {
                    return Arrays.equals(sceneObject.getDefinition().getNewColors(), GEM_COLORS);
                }) != null)
                .addSubActivity(() -> {
                    int count = Inventory.getCount();
                    SceneObjects.getNearest(sceneObject -> {
                        return Arrays.equals(sceneObject.getDefinition().getNewColors(), GEM_COLORS);
                    }).interact("Mine");
                    Time.sleepUntil(() -> Inventory.getCount() > count, 100, 1000 * 30);
                })
                .onlyOnce()
                .build();

        Activity bank = Activity.newBuilder()
                .withName("Banking")
                .addPreReq(Inventory::isFull)
                .addSubActivity(Activities.moveTo(bankers))
                .addSubActivity(() -> Npcs.getNearest("Banker").interact("Bank"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 5000))
                .addSubActivity(() -> Bank.depositInventory())
                .addSubActivity(() -> Time.sleepUntil(Inventory::isEmpty, 3000))
                .addSubActivity(() -> Bank.close())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(Activities.moveTo(rocks))
                .onlyOnce()
                .build();


        return Activity.newBuilder()
                .withName("Mining gems")
                .addSubActivity(hopIfBusy)
                .addSubActivity(mine)
                .addSubActivity(bank)
                .build();
    }

    // TODO(dmattia): refactor
    public static Activity mineAndDrop(Rock rockType) {
        return Activity.newBuilder()
                .withName("Mining and dropping some rocks")
                .addPreReq(() -> !Inventory.isFull())
                .addPreReq(() -> SceneObjects.getNearest(sceneObject -> {
                    return Arrays.equals(sceneObject.getDefinition().getNewColors(), rockType.getColors());
                }) != null)
                .addSubActivity(() -> {
                    int count = Inventory.getCount(rockType.getOre());
                    SceneObjects.getNearest(sceneObject -> {
                        return Arrays.equals(sceneObject.getDefinition().getNewColors(), rockType.getColors());
                    }).interact("Mine");
                    Time.sleepUntil(() -> Inventory.getCount(rockType.getOre()) > count, 100, 1000 * 30);
                })
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(268)))
                .untilPreconditionsFail()
                .build()
                .andThen(Activities.dropAll(rockType.getOre()));
    }

    public enum Rock {
        COPPER(COPPER_COLORS, "Copper ore"),
        IRON(IRON_COLORS, "Iron ore");

        private short[] colors;
        private String ore;

        Rock(short[] colors, String ore) {
            this.colors = colors;
            this.ore = ore;
        }

        public short[] getColors() {
            return colors;
        }

        public String getOre() {
            return ore;
        }
    }
}
