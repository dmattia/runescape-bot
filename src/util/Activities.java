package util;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Pickable;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.commons.BankLocation;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.Production;
import org.rspeer.runetek.api.component.WorldHopper;
import org.rspeer.runetek.api.component.tab.Equipment;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Tab;
import org.rspeer.runetek.api.component.tab.Tabs;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Pickables;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.WorldChangeListener;
import org.rspeer.ui.Log;
import trade.PriceSummary;
import trade.Prices;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Activities {
    private static final int INVENTORY_SIZE = 28;

    public static Activity debug(Supplier<String> s) {
        return Activity.newBuilder()
                .addSubActivity(() -> System.out.println("Debug: " + s.get()))
                .onlyOnce()
                .build();
    }

    public static Activity goToBank() {
        return Activity.newBuilder()
                .withName("Moving to bank")
                .addPreReq(() -> BankLocation.getNearest().getPosition().distance(Players.getLocal()) > 10)
                .addSubActivity(moveTo(BankLocation.getNearest().getPosition()))
                .onlyOnce()
                .build();
    }

    public static Activity switchToTab(Tab tab) {
        return Activity.newBuilder()
                .addPreReq(() -> !Tabs.isOpen(tab))
                .addSubActivity(() -> Tabs.open(tab))
                .addSubActivity(() -> Time.sleepUntil(() -> Tabs.isOpen(tab), 134, 1000))
                .build();
    }

    public static Activity moveTo(Position position) {
        return Activity.newBuilder()
                .withName("Moving to position: " + position)
                .addPreReq(() -> position.distance(Players.getLocal()) > 5)
                .addSubActivity(Activity.newBuilder()
                        .addPreReq(() -> Movement.getRunEnergy() == 100)
                        .addSubActivity(() -> Movement.toggleRun(true))
                        .onlyOnce()
                        .build())
                .addSubActivity(() -> Movement.walkToRandomized(position))
                .addSubActivity(() -> Time.sleepWhile(Movement::isDestinationSet, 1000 * 10))
                .untilPreconditionsFail()
                .build();
    }

    public static Activity moveTo(Area area) {
        return moveTo(area.getCenter());
    }

    public static Activity use(String name) {
        return Activity.newBuilder()
                .withName("Using " + name)
                .addSubActivity(use(item -> item.getName().equals(name)))
                .onlyOnce()
                .build();
    }

    public static Activity use(Predicate<Item> predicate) {
        return Activity.newBuilder()
                .addPreReq(() -> Inventory.contains(predicate))
                .addSubActivity(() -> Inventory.getFirst(predicate).interact("Use"))
                .addSubActivity(pauseFor(Duration.ofMillis(180)))
                .onlyOnce()
                .build();
    }

    /**
     * Uses a source item on a destination item from the inventory.
     * ex) use("Ranarr weed", "Vial of Water") will use the ranarr item on a vial of water.
     * If another action (such as producing some item or choosing an amount to create) is needed,
     * that should be handled by a separate activity. See `produceAll` for an example.
     */
    public static Activity use(String source, String destination) {
        return Activity.newBuilder()
                .withName("Using " + source + " on " + destination)
                .addPreReq(() -> Inventory.contains(source))
                .addPreReq(() -> Inventory.contains(destination))
                .addSubActivity(deselectItem())
                .addSubActivity(use(source))
                .addSubActivity(use(destination))
                .onlyOnce()
                .build();
    }

    /**
     * Ensures no item in the inventory is selected.
     */
    public static Activity deselectItem() {
        return Activity.newBuilder()
                .withName("Deselecting item")
                .addPreReq(Inventory::isItemSelected)
                .addSubActivity(() -> Inventory.deselectItem())
                .addSubActivity(() -> Time.sleepWhile(Inventory::isItemSelected, 60, 500))
                .onlyOnce()
                .build();
    }

    public static Activity hopWorlds() {
        return Activity.newBuilder()
                .withName("Hopping worlds")
                .addSubActivity(() -> Tabs.open(Tab.LOGOUT))
                .addSubActivity(() -> Time.sleepUntil(() -> Tabs.isOpen(Tab.LOGOUT), 300))
                .addSubActivity(
                        Activity.newBuilder()
                                .withName("Opening world switcher")
                                .addPreReq(() -> Interfaces.firstByAction(action -> action.equals("World Switcher")) != null)
                                .addSubActivity(() -> Interfaces.firstByAction(action -> action.equals("World Switcher")).interact("World Switcher"))
                                .addSubActivity(pauseFor(Duration.ofMillis(582)))
                                .onlyOnce()
                                .build()
                )
                .addSubActivity(() -> WorldHopper.randomHopInP2p())
                .addSubActivity(() -> {
                    AtomicBoolean complete = new AtomicBoolean(false);
                    WorldChangeListener listener = event -> complete.set(true);

                    Game.getEventDispatcher().register(listener);
                    Time.sleepUntil(() -> complete.get(), 286, 1000 * 10);
                    Game.getEventDispatcher().deregister(listener);
                })
                .addSubActivity(pauseFor(Duration.ofSeconds(3)))
                .build();
    }


    /**
     * Sleeps for a duration, roughly. To keep Jagex on its toes, the actual duration waited for is randomly between
     * the input duration and 20% longer than that duration.
     */
    public static Activity pauseFor(Duration duration) {
        return Activity.newBuilder()
                .withName("Pausing for roughly " + duration.toMillis() + " ms")
                .addSubActivity(() -> Time.sleep(duration.toMillis(), duration.toMillis() * 120 / 100))
                .build();
    }

    /**
     * Opens the banking interface, assuming the local player is already near a banker. This does not care what
     * kind of bank is nearby, as long as the banker is "Banker" and the interaction is "Bank".
     */
    public static Activity openBank() {
        return Activity.newBuilder()
                .withName("Opening bank")
                .addPreReq(() -> !Bank.isOpen() && BankLocation.getNearestWithdrawable() != null)
                .addSubActivity(() -> {
                    BankLocation bank = BankLocation.getNearestWithdrawable();

                    switch (bank.getType()) {
                        case BANK_BOOTH:
                            SceneObjects.getNearest("Bank booth").interact("Bank");
                            break;
                        case NPC:
                            Npcs.getNearest(bank.getName().replace("Emerald Benedicht", "Emerald Benedict")).interact("Bank");
                            break;
                        case BANK_CHEST:
                            SceneObjects.getNearest("Bank chest").interact("Use");
                            break;
                        case DEPOSIT_BOX:
                            Log.severe("Cannot open deposit box");
                            Globals.script.setStopping(true);
                            break;
                    }
                })
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 1000 * 5))
                .onlyOnce()
                .build()
                .addPreActivity(goToBank());
    }

    /**
     * Closes the banking interface if it is open.
     */
    public static Activity closeBank() {
        return Activity.newBuilder()
                .withName("Closing bank")
                .addPreReq(Bank::isOpen)
                .addSubActivity(() -> Bank.close())
                .addSubActivity(() -> Time.sleepWhile(Bank::isOpen, 1000))
                .onlyOnce()
                .build();
    }

    /**
     * Clicks on an item repeatedly in the inventory until none remain. It is assumed that the passed in item
     * should disappear after being consumed, or at least change to a different item (ex: cleaning herbs).
     */
    public static Activity consumeAll(String itemName) {
        return Activity.newBuilder()
                .withName("Consuming all " + itemName)
                .addPreReq(() -> Inventory.contains(itemName))
                .addSubActivity(() -> {
                    int initialCount = Inventory.getCount(itemName);
                    Inventory.getFirst(itemName).click();
                    Time.sleepUntil(() -> Inventory.getCount(itemName) < initialCount, 1000);
                })
                .untilPreconditionsFail()
                .build();
    }

    /**
     * Deposits all items in the inventory into a banking interface.
     * <p>
     * Leaves the banking interface open.
     */
    public static Activity depositInventory() {
        return Activity.newBuilder()
                .withName("Depositing all items")
                .addPreReq(() -> !Inventory.isEmpty())
                .addSubActivity(() -> Bank.depositInventory())
                .addSubActivity(pauseFor(Duration.ofMillis(300)))
                .onlyOnce()
                .build()
                .addPreActivity(openBank());
    }

    public static Activity depositWornItems() {
        return Activity.newBuilder()
                .withName("Depositing all worn items")
                .addPreReq(() -> Equipment.getOccupiedSlots().length > 0)
                .addSubActivity(() -> Bank.depositEquipment())
                .addSubActivity(pauseFor(Duration.ofMillis(300)))
                .onlyOnce()
                .build()
                .addPreActivity(openBank());
    }

    public static Activity depositAll(String item) {
        return Activity.newBuilder()
                .withName("Depositing all " + item)
                .addPreReq(() -> Inventory.contains(item))
                .addSubActivity(() -> Bank.depositAll(item))
                .addSubActivity(pauseFor(Duration.ofMillis(300)))
                .onlyOnce()
                .build()
                .addPreActivity(openBank());
    }

    public static Activity withdrawEqualOf(String... items) {
        return Stream.of(items)
                .map(item -> withdraw(item, INVENTORY_SIZE / items.length))
                .collect(new ActivityCollector())
                .addPreActivity(depositInventory())
                .andThen(closeBank());
    }

    /**
     * Withdraws a certain amount of an item from a banking interface, assuming the interface is already open.
     * <p>
     * Leaves the banking interface open.
     */
    public static Activity withdraw(String itemName, int amount) {
        return Activity.newBuilder()
                .withName("Withdrawing " + amount + " of " + itemName)
                .addPreReq(Bank::isOpen)
                .addSubActivity(() -> Bank.withdraw(itemName, amount))
                .addSubActivity(pauseFor(Duration.ofMillis(200)))
                .addSubActivity(() -> {
                    if (Inventory.getCount(true, itemName) < amount) Globals.script.setStopping(true);
                })
                .onlyOnce()
                .build()
                .addPreActivity(openBank());
    }

    /**
     * Produces a given item assuming the Production menu is open or will soon be open.  This activity completes
     * immediately after starting production.
     */
    public static Activity produceAll(String item) {
        Activity waitForProductionToOpen = Activity.newBuilder()
                .addSubActivity(() -> Time.sleepUntil(Production::isOpen, 1000 * 2))
                .build();

        return Activity.newBuilder()
                .withName("Producing all " + item)
                .addPreReq(Production::isOpen)
                .addSubActivity(() -> Production.setAmount(Production.Amount.ALL))
                .addSubActivity(() -> Production.initiate(item))
                .addSubActivity(() -> Production.initiate())
                .addSubActivity(pauseFor(Duration.ofSeconds(1)))
                .build()
                .addPreActivity(waitForProductionToOpen);
    }

    public static Activity dropAll(Predicate<Item> itemPredicate) {
        return Activity.newBuilder()
                .addPreReq(() -> Inventory.contains(itemPredicate))
                .addSubActivity(() -> {
                    int count = Inventory.getCount(itemPredicate);
                    Inventory.getFirst(itemPredicate).interact("Drop");
                    Time.sleepUntil(() -> Inventory.getCount(itemPredicate) < count, 2000);
                })
                .untilPreconditionsFail()
                .build();
    }

    public static Activity dropAll(String itemName) {
        return dropAll(item -> item.getName().equalsIgnoreCase(itemName));
    }

    public static Activity dropEverything() {
        return dropAll(item -> true);
    }

    public static Activity equip(String item) {
        return Activity.newBuilder()
                .addPreReq(() -> !Equipment.contains(item))
                .addPreReq(() -> Inventory.contains(item))
                .addSubActivity(() -> Inventory.getFirst(item).click())
                .addSubActivity(() -> Time.sleepUntil(() -> Equipment.contains(item), 1000 * 3))
                .build();
    }

    public static Activity wearGraceful() {
        return Activity.newBuilder()
                .addPreReq(() -> !Equipment.contains("Graceful top"))
                .addSubActivity(Activities.withdraw("Graceful top", 1))
                .addSubActivity(Activities.withdraw("Graceful hood", 1))
                .addSubActivity(Activities.withdraw("Graceful legs", 1))
                .addSubActivity(Activities.withdraw("Graceful cape", 1))
                .addSubActivity(Activities.withdraw("Graceful gloves", 1))
                .addSubActivity(Activities.withdraw("Graceful boots", 1))
                .addSubActivity(Activities.closeBank())

                .addSubActivity(equip("Graceful top"))
                .addSubActivity(equip("Graceful hood"))
                .addSubActivity(equip("Graceful legs"))
                .addSubActivity(equip("Graceful cape"))
                .addSubActivity(equip("Graceful gloves"))
                .addSubActivity(equip("Graceful boots"))

                .onlyOnce()
                .build();
    }

    // TODO(dmattia): Refactor this to use pickup somehow
    public static Activity pickupAll(String... itemNames) {
        return Activity.newBuilder()
                .withName("Picking up all " + Arrays.stream(itemNames).collect(Collectors.joining(", ")))
                .addPreReq(() -> {
                    Pickable item = Pickables.getNearest(itemNames);
                    if (item == null) return false;
                    return item.distance(Players.getLocal()) < 10;
                })
                .addPreReq(() -> !Inventory.isFull())
                .addSubActivity(() -> {
                    int count = Inventory.getCount(itemNames);
                    Pickables.getNearest(itemNames).interact("Take");
                    Time.sleepUntil(() -> Inventory.getCount(itemNames) > count, 1000 * 5);
                })
                .untilPreconditionsFail()
                .build();
    }

    public static Activity pickupItemsValuedOver(int minValue) {
        return Activity.newBuilder()
                .withName("Picking up valuable items worth over: " + minValue + " gp")
                .addSubActivity(() -> {
                    for (Pickable pickable : Pickables.getLoaded()) {
                        if (Prices.getPriceSummary(pickable.getName()).map(PriceSummary::getSellAverage).orElse(0) > minValue) {
                            pickup(pickable.getName()).run();
                        }
                    }
                })
                .build();
    }

    /**
     * Picks up an item off the ground, assuming it is already present or will be within 5 seconds.
     */
    public static Activity pickup(String itemName) {
        return Activity.newBuilder()
                .withName("Picking up " + itemName)
                .addPreReq(() -> Pickables.getNearest(itemName) != null && !Inventory.isFull())
                .addSubActivity(() -> {
                    int count = Inventory.getCount(itemName);
                    Pickables.getNearest(itemName).interact("Take");
                    Time.sleepUntil(() -> Inventory.getCount(itemName) > count, 1000 * 5);
                })
                .onlyOnce()
                .build();
    }
}
