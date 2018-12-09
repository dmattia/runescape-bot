package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

public class Woodcutting {
    private static final Position OAK_LOCATION = new Position(1620, 3510);

    /**
     * Cuts trees in the woodcutting guild.
     */
    public static Activity cut(Tree tree) {
        return Activity.newBuilder()
                .addPreReq(() -> !Inventory.isFull())
                .addSubActivity(Activities.moveTo(tree.getLocation()))
                .addPreReq(() -> SceneObjects.getNearest(tree.getTree()) != null)
                .addSubActivity(() -> SceneObjects.getNearest(tree.getTree()).interact("Chop down"))
                .addSubActivity(() -> Time.sleepWhile(() -> Players.getLocal().isAnimating(), 1000 * 60 * 5))
                .untilPreconditionsFail()
                .build();
    }

    public enum Tree {
        OAK("Oak", "Oak logs", OAK_LOCATION);

        private final String tree;
        private final String log;
        private final Position location;

        Tree(String tree, String log, Position location) {
            this.tree = tree;
            this.log = log;
            this.location = location;
        }

        public Position getLocation() {
            return location;
        }

        public String getLog() {
            return log;
        }

        public String getTree() {
            return tree;
        }
    }




         /*
        Activity.newBuilder()
                // walk to trees
                .addSubActivity(SubActivity.newBuilder()
                        .onlyIf(() -> Locations.WILLOW_TREES.distance(Players.getLocal()) > 10)
                        .setJob(() -> Movement.walkToRandomized(Locations.WILLOW_TREES))
                        .betweenJobs(() -> Time.sleepWhile(Movement::isDestinationSet, 1000 * 5))
                        .continueWhile(() -> Locations.WILLOW_TREES.distance(Players.getLocal()) > 5)
                        .build())
                // fill inventory with logs
                .addSubActivity(SubActivity.newBuilder()
                        .onlyIf(() -> !Inventory.isFull())
                        .setJob(() -> SceneObjects.getNearest("Willow").interact("Chop down"))
                        .betweenJobs(() -> Time.sleepWhile(() -> Players.getLocal().isAnimating(), 1000 * 60 * 5))
                        .stopWhen(Inventory::isFull)
                        .build())
                // make arrow shafts
                .addSubActivity(SubActivity.newBuilder()
                        .setJob(() -> {
                            use(KNIFE, WILLOW_LOG);
                            Time.sleepUntil(Production::isOpen, 2000);
                            Production.initiate(ARROW_SHAFT);
                            Production.initiate();
                        })
                        .thenWaitUntil(() -> !Players.getLocal().isAnimating() && !Inventory.contains(WILLOW_LOG))
                        .build())
                .build()
                .perform();
        */
}
