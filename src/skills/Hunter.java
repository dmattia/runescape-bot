package skills;

import org.rspeer.runetek.adapter.component.InterfaceComponent;
import org.rspeer.runetek.api.commons.Streams;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.Production;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.*;
import util.Activities;
import util.Predicates;
import util.common.Activity;
import util.common.ActivityCollector;
import util.common.ActivityConfigModel;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Hunter {
    public static Activity birdhouseRun() {
        Area FOSSIL_ISLAND = Area.rectangular(3624, 3903, 3849, 3697);
        BooleanSupplier ON_FOSSIL_ISLAND = () -> {
            Position basePosition = new Position(Players.getLocal().getX(), Players.getLocal().getY(), 0);
            return FOSSIL_ISLAND.contains(basePosition);
        };

        Position NORTH_WEST_HOUSE = new Position(3677, 3882);
        Position SOUTH_WEST_HOUSE = new Position(3680, 3814);
        int INTERFACE_ID = 608;

        ActivityConfigModel model = ActivityConfigModel.newBuilder()
                .withTextField("Log", "Logs")
                .withTextField("Birdhouse", "Bird house")
                .withTextField("Seed", "Asgarnian seed")
                .build();

        Activity goToHouse = Activity.newBuilder()
                .withName("Going to house")
                .addPreReq(Predicates.not(House::isInside))
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").click())
                .thenSleepUntil(House::isInside)
                .tick()
                .build();

        Activity digsitePendant = Activity.newBuilder()
                .withName("Taking digsite pendant tele")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Digsite Pendant") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Digsite Pendant").interact("Fossil Island"))
                .thenSleepUntil(() -> FOSSIL_ISLAND.contains(Players.getLocal()))
                .tick()
                .build();

        Activity goToFossilIsland = Activity.newBuilder()
                .addPreReq(Predicates.not(ON_FOSSIL_ISLAND))
                .addSubActivity(goToHouse)
                .addSubActivity(digsitePendant)
                .build();

        Activity useMushTree = Activity.newBuilder()
                .withName("Using Mush Tree")
                .addPreReq(ON_FOSSIL_ISLAND)
                .addPreReq(() -> SceneObjects.getNearest("Magic Mushtree") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Magic Mushtree").interact("Use"))
                .thenSleepUntil(() -> Interfaces.isOpen(INTERFACE_ID))
                .tick()
                .build();

        Function<MushTree, Activity> goToMushTree = tree -> Activity.newBuilder()
                .withName("Using mushtree tele to " + tree.name())
                .addPreReq(ON_FOSSIL_ISLAND)
                .addPreReq(() -> Interfaces.isOpen(INTERFACE_ID))
                .addSubActivity(() -> tree.clickComponent())
                .thenPauseFor(Duration.ofSeconds(3))
                .build();

        Activity makeHouses = Activity.of(() -> {
            Arrays.stream(SceneObjects.getLoaded())
                    .filter(obj -> obj.getName().equals("Birdhouse"))
                    .map(birdHouse -> Activity.newBuilder()
                            .withName("Building birdhouse")
                            .withConfigModel(model)
                            .addSubActivity(() -> birdHouse.interact("Empty"))
                            .completeAnimation()
                            .tick()
                            .addSubActivity(config -> Activities.use("Clockwork", "Logs").run())
                            .completeAnimation()
                            .tick()
                            .addSubActivity(config -> Activities.use(config.get("Birdhouse")).run())
                            .addSubActivity(() -> SceneObjects.getNearest("Space").click())
                            .completeAnimation()
                            .addSubActivity(() -> SceneObjects.getNearest("Birdhouse (empty)").click())
                            .completeAnimation()
                            .tick()
                            .build())
                    .collect(new ActivityCollector())
                    .withName("Making houses in this area")
                    .build()
                    .run();;
        });

        return Activity.newBuilder()
                .withName("Birdhouse run")
                .withConfigModel(model)
                .addPreReq(config -> Inventory.contains(config.get("Seed")))
                .addPreReq(config -> Inventory.contains(config.get("Log")))
                .addPreReq(() -> Inventory.contains("Hammer"))
                .addPreReq(() -> Inventory.contains("Chisel"))
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                /*
                .addSubActivity(goToFossilIsland)
                .addSubActivity(useMushTree)
                .addSubActivity(goToMushTree.apply(MushTree.VERDANT_VALLEY))
                .thenPauseFor(Duration.ofSeconds(3))
                */
                // .addSubActivity(makeHouses)
                .addSubActivity(() -> {
                    // SceneObjects.getNearest("Space").interact("Empty");

                    //System.out.println(SceneObjects.getNearest(so -> so.containsAction("Empty")));
                })
                .thenPauseFor(Duration.ofSeconds(3))
                .build();
    }

    public enum MushTree {
        HOUSE_ON_THE_HILL(0),
        VERDANT_VALLEY(1),
        MUSHROOM_MEADOW(3);

        private final int index;

        MushTree(int index) {
            this.index = index;
        }

        public void clickComponent() {
            Arrays.stream(Interfaces.get(608))
                    // There is a bug in the current api, this should never throw an NPE.
                    .filter(component -> {
                        try {
                            return component.getHoverListeners().length > 0;
                        } catch (NullPointerException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList())
                    .get(index)
                    .click();
        }
    }
}
