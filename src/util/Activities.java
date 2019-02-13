package util;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.Pickable;
import org.rspeer.runetek.api.Game;
import org.rspeer.runetek.api.Varps;
import org.rspeer.runetek.api.commons.BankLocation;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.Production;
import org.rspeer.runetek.api.component.WorldHopper;
import org.rspeer.runetek.api.component.tab.*;
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
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Activities {
    private static final int INVENTORY_SIZE = 28;

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
                .withName("Switching to tab: " + tab.toString())
                .addPreReq(() -> !Tabs.isOpen(tab))
                .addSubActivity(() -> Tabs.open(tab))
                .addSubActivity(Activities.sleepUntil(() -> Tabs.isOpen(tab)))
                .build();
    }

    public static Activity toggleRun() {
        return Activity.newBuilder()
                .withName("Turning run on")
                .addPreReq(() -> Movement.getRunEnergy() == 100)
                .addPreReq(() -> !Movement.isRunEnabled())
                .addSubActivity(() -> Movement.toggleRun(true))
                .onlyOnce()
                .build();
    }

    public static Activity moveToExactly(Position position) {
        return Activity.newBuilder()
                .withName("Moving to the exact position: " + position)
                .addPreReq(() -> Players.getLocal().distance(position) < 100)
                .addSubActivity(() -> (Movement.setWalkFlag(position)))
                .addSubActivity(Activities.sleepWhile(Movement::isDestinationSet))
                .build();
    }

    public static Activity moveTo(Position position) {
        return Activity.newBuilder()
                .withName("Moving to position: " + position)
                .addPreReq(() -> position.distance(Players.getLocal()) > 5)
                .addSubActivity(toggleRun())
                //.addSubActivity(() -> Movement.walkToRandomized(position))
                .addSubActivity(() -> Movement.walkTo(position))
                //.addSubActivity(magicImbue())
                .addSubActivity(Activities.sleepUntil(() -> !Movement.isDestinationSet() ||
                        Movement.getDestination().distance() < 4))
                //.withoutPausingBetweenActivities()
                .untilPreconditionsFail()
                .build();
    }

    /**
     * Casts magic imbue if it isn't already active. For now, this requires you to have a steam battlestaff and a rune
     * pouch because that's what I always have when I imbue.
     * TODO(dmattia): Make a better casting system that verifies rune prereqs
     */
    public static Activity magicImbue() {
        int magicImbueVarpIndex = 5438;

        return Activity.newBuilder()
                .addPreReq(() -> Varps.getBitValue(magicImbueVarpIndex) == 0)
                .addPreReq(() -> EquipmentSlot.MAINHAND.getItemName().equals("Steam battlestaff"))
                .addPreReq(() -> Inventory.contains("Rune pouch"))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.MAGIC_IMBUE))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .build();
    }

    // TODO(dmattia): This should not just call moveTo(Position), as the logic for being close is different
    public static Activity moveTo(Area area) {
        return moveTo(area.getCenter());
    }

    public static Activity use(String name) {
        return Activity.newBuilder()
                .withName("Using " + name)
                .addSubActivity(use(item -> item.getName().equalsIgnoreCase(name)))
                .onlyOnce()
                .build();
    }

    public static Activity use(Predicate<Item> predicate) {
        return Activity.newBuilder()
                .withName("using item")
                .addPreReq(() -> Inventory.contains(predicate))
                .addSubActivity(() -> Inventory.getFirst(predicate).interact("Use"))
                .addSubActivity(pauseFor(Duration.ofMillis(100)))
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
                .addSubActivity(Activities.sleepWhile(Inventory::isItemSelected))
                .onlyOnce()
                .build();
    }

    public static Activity hopWorlds() {
        return Activity.newBuilder()
                .withName("Hopping worlds")
                .addSubActivity(() -> Tabs.open(Tab.LOGOUT))
                .addSubActivity(Activities.sleepUntil(() -> Tabs.isOpen(Tab.LOGOUT)))
                .addSubActivity(
                        Activity.newBuilder()
                                .withName("Opening world switcher")
                                .addPreReq(() -> Interfaces.firstByAction(action -> action.equalsIgnoreCase("World Switcher")) != null)
                                .addSubActivity(() -> Interfaces.firstByAction(action -> action.equalsIgnoreCase("World Switcher")).interact("World Switcher"))
                                .addSubActivity(pauseFor(Duration.ofMillis(582)))
                                .onlyOnce()
                                .build()
                )
                .addSubActivity(() -> WorldHopper.hopNext(Predicates::worldIsSafe))
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
                .addSubActivity(() -> Activities.moveTo(BankLocation.getNearestWithdrawable().getPosition()).run())
                .addSubActivity(() -> {
                    BankLocation bank = BankLocation.getNearestWithdrawable();

                    switch (bank.getType()) {
                        case BANK_BOOTH:
                            SceneObjects.getNearest("Bank booth").interact("Bank");
                            break;
                        case NPC:
                            Npcs.getNearest(bank.getName()).interact("Bank");
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
                .addSubActivity(Activities.sleepUntil(Bank::isOpen))
                .onlyOnce()
                .build();
    }

    /**
     * Closes the banking interface if it is open.
     */
    public static Activity closeBank() {
        return Activity.newBuilder()
                .withName("Closing bank")
                .addPreReq(Bank::isOpen)
                .addSubActivity(() -> Bank.close())
                .addSubActivity(Activities.sleepWhile(Bank::isOpen))
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

    public static Activity depositAll(String itemName) {
        return Activity.newBuilder()
                .withName("Depositing all " + itemName)
                .addSubActivity(depositAll(item -> item.getName().equalsIgnoreCase(itemName)))
                .build();
    }

    public static Activity depositAll(Predicate<Item> predicate) {
        return Activity.newBuilder()
                .addPreReq(Bank::isOpen)
                .addPreReq(() -> Inventory.contains(predicate))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(110)))
                .addSubActivity(() -> Bank.depositAll(predicate))
                .addSubActivity(Activities.sleepWhile(() -> Inventory.contains(predicate)))
                .withoutPausingBetweenActivities()
                .onlyOnce()
                .build();
    }

    public static Activity withdrawEqualOf(String... items) {
        return Stream.of(items)
                .map(item -> withdraw(item, INVENTORY_SIZE / items.length))
                .collect(new ActivityCollector())
                .build()
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
                .addSubActivity(withdraw(item -> item.getName().equalsIgnoreCase(itemName), amount))
                .build();
    }

    public static Activity withdrawAll(String itemName) {
        return Activity.newBuilder()
                .withName("Withdrawing all of " + itemName)
                .addSubActivity(withdrawAll(item -> item.getName().equalsIgnoreCase(itemName)))
                .build();
    }

    public static Activity withdrawAll(Predicate<Item> predicate) {
        return Activity.newBuilder()
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(110)))
                .addSubActivity(() -> {
                    int currentAmount = Inventory.getCount(true, predicate);
                    Bank.withdrawAll(predicate);
                    Activities.sleepUntil(() -> Inventory.getCount(true, predicate) > currentAmount).run();
                })
                .withoutPausingBetweenActivities()
                .build();
    }

    public static Activity withdraw(Predicate<Item> predicate, int amount) {
        return Activity.newBuilder()
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(110)))
                .addSubActivity(() -> Bank.withdraw(predicate, amount))
                .addSubActivity(Activities.sleepUntil(() -> Inventory.getCount(true, predicate) >= amount))
                .withoutPausingBetweenActivities()
                .build()
                .andThen(stopScriptIf(() -> Inventory.getCount(true, predicate) < amount));
    }

    public static Activity stopScriptIf(BooleanSupplier condition) {
        return Activity.newBuilder()
                .addPreReq(condition)
                .addSubActivity(() -> Globals.script.setStopping(true))
                .build();
    }

    public static Activity sleepWhile(BooleanSupplier condition) {
        return Activity.newBuilder()
                .addPreReq(condition)
                .addSubActivity(pauseFor(Duration.ofMillis(50)))
                .maximumDuration(Duration.ofMinutes(2))
                .untilPreconditionsFail()
                .build()
                .andThen(stopScriptIf(condition));
    }

    public static Activity sleepUntil(BooleanSupplier condition) {
        return sleepWhile(Predicates.not(condition));
    }

    /**
     * Produces a given item assuming the Production menu is open or will soon be open.  This activity completes
     * immediately after starting production.
     */
    public static Activity produceAll(String item) {
        Activity waitForProductionToOpen = Activity.newBuilder()
                .addSubActivity(Activities.sleepUntil(Production::isOpen))
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
                .addSubActivity(Activities.sleepUntil(() -> Equipment.contains(item)))
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
                .addPreReq(() -> Pickables.getNearest(item -> item.getName().contains(itemName)) != null && !Inventory.isFull())
                .addSubActivity(() -> {
                    int count = Inventory.getCount(item -> item.getName().contains(itemName));
                    Pickables.getNearest(item -> item.getName().contains(itemName)).interact("Take");
                    Activities.sleepUntil(() -> Inventory.getCount(item -> item.getName().contains(itemName)) > count).run();
                })
                .onlyOnce()
                .build();
    }
}
