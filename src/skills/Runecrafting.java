package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.movement.transportation.FairyRing;
import org.rspeer.runetek.api.scene.House;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Runecrafting {
    private static final Area AIR_ALTAR = Area.rectangular(2982, 3288, 2988, 3294);
    private static final Area ZANARIS = Area.rectangular(2309, 4347, 2510, 4492);

    private static Activity repairPouches() {
       return Activity.newBuilder()
                .withName("Repairing pouches via npc contact")
                .addSubActivity(Activities.withdraw("Astral rune", 1))
                .addSubActivity(Activities.withdraw("Cosmic rune", 1))
                .addSubActivity(Activities.withdraw("Air rune", 2))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.NPC_CONTACT))
                .addSubActivity(() -> Time.sleepUntil(() ->
                        Interfaces.firstByAction(action -> action.equalsIgnoreCase("Dark mage")) != null, 5000))
                .addSubActivity(() -> Interfaces.firstByAction(action -> action.equalsIgnoreCase("Dark mage")).click())
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1200, 8000))
                .addSubActivity(() -> Dialog.processContinue())
                .addSubActivity(() -> Dialog.processContinue())
                .addSubActivity(() -> Dialog.processContinue())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .build();
    }

    private static Activity makeSingleNatureRun() {
        Activity getEss = Activity.newBuilder()
                .withName("Withdrawing rune essence")
                .addPreReq(() -> !Inventory.contains("Pure essence"))
                .addSubActivity(Activities.depositAll("Nature rune"))
                .addSubActivity(Activities.withdraw("Nature talisman", 1))
                .addSubActivity(Activities.withdraw("Pure essence", 23))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .map(item -> Activity.of(() -> item.interact("Fill")))
                            .collect(new ActivityCollector())
                            .run();
                })
                .addSubActivity(Activities.withdraw("Pure essence", 23))
                .addSubActivity(Activities.closeBank())
                .build();

        Activity goToAltar = Activity.newBuilder()
                .withName("Going to Nature altar")
                .addSubActivity(Activities.moveTo(FairyRing.getNearest().getPosition()))
                .addSubActivity(() -> SceneObjects.getNearest("Fairy ring").interact("Last-Destination (CKR)"))
                .addSubActivity(() -> Time.sleepUntil(() -> FairyRing.getNearest().getCode().equals("CKR"), 524, 5000))
                .addSubActivity(Activities.moveTo(Type.NATURE.getPosition()))
                .build();

        Activity craftRunes = Activity.newBuilder()
                .withName("Crafting some runes")
                .addSubActivity(Activities.use(Type.NATURE.getTalisman()))
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious Ruins").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(() ->
                        Type.NATURE.getPosition().distance(Players.getLocal()) > 500, 5000))

                .addSubActivity(Activities.use("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Pure Essence"), 1000 * 15))

                // Pause in case a level up dialog appears
                .addSubActivity(Activities.pauseFor(Duration.ofSeconds(2)))
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .map(item -> Activity.of(() -> item.interact("Empty")))
                            .collect(new ActivityCollector())
                            .run();
                })
                .addSubActivity(Activities.use("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Pure Essence"), 1000 * 15))

                // Pause in case a level up dialog appears
                .addSubActivity(Activities.pauseFor(Duration.ofSeconds(2)))
                .build();

        Activity goHome = Activity.newBuilder()
                .withName("Going home & handling business there")
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").click())
                .addSubActivity(() -> Time.sleepUntil(House::isInside, 650, 1000 * 12))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(426)))
                .addSubActivity(() -> SceneObjects.getNearest(so -> so.getName().startsWith("Pool")).click())
                .addSubActivity(() -> Time.sleepUntil(() -> Movement.getRunEnergy() == 100, 365, 1000 * 15))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(500)))
                .addSubActivity(() -> SceneObjects.getNearest("Amulet of Glory").interact("Edgeville"))
                .addSubActivity(() -> Time.sleepWhile(House::isInside, 10000))
                .build();

        return Activity.newBuilder()
                .withName("Making rune run")
                .addSubActivity(getEss)
                .addSubActivity(goToAltar)
                .addSubActivity(craftRunes)
                .addSubActivity(goHome)
                .onlyOnce()
                .build();
    }

    /**
     * Crafts natures safely on Karamja. Uses the general store method.
     */
    public static Activity craftNatureRunes() {
        return Activity.newBuilder()
                .withName("Nature running")
                .addSubActivity(() -> {
                    IntStream.rangeClosed(1, 20)
                            .mapToObj(runNumber ->
                              Activity.newBuilder()
                                      .withName("Nature running: run number " + runNumber)
                                      .addSubActivity(makeSingleNatureRun())
                                      .onlyOnce()
                                      .build()
                            )
                            .collect(new ActivityCollector())
                            .run();
                })
                .addSubActivity(repairPouches())
                .untilPreconditionsFail()
                .build();
    }


    public static Activity goToZanaris() {
        Position zanarisEntrance = new Position(3200, 3169);

        Activity prepEquipment = Activity.newBuilder()
                .withName("Getting runecrafting equipment")
                .addPreReq(() -> !Inventory.contains("Dramen staff") && !Equipment.contains("Dramen staff"))
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                .addPreReq(() -> Equipment.contains("Graceful hood"))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.depositWornItems())
                .addSubActivity(Activities.withdraw("Dramen staff", 1))
                .addSubActivity(Activities.withdraw("Teleport to house", 1))
                .addSubActivity(Activities.wearGraceful())
                .addSubActivity(Activities.equip("Dramen staff"))
                .onlyOnce()
                .build();

        Activity gotoLumbridge = Activity.newBuilder()
                .withName("Going to Lumbridge")
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").interact("Break"))
                .addSubActivity(() -> Time.sleepUntil(House::isInside, 1000 * 5))
                .addSubActivity(() -> SceneObjects.getNearest("Lumbridge Portal").interact("Enter"))
                .addSubActivity(() -> Time.sleepWhile(House::isInside, 269, 1000 * 10))
                .onlyOnce()
                .build();

        return Activity.newBuilder()
                .withName("Going to Zanaris")
                .addPreReq(() -> !ZANARIS.contains(Players.getLocal()))
                .addSubActivity(prepEquipment)
                .addSubActivity(gotoLumbridge)
                .addSubActivity(Activities.moveTo(zanarisEntrance))
                .addSubActivity(() -> SceneObjects.getNearest("Door").interact("Open"))
                .addSubActivity(() -> Time.sleepUntil(() -> ZANARIS.contains(Players.getLocal()), 1000 * 8))
                .build();
    }

    /**
     * Cosmic runes are weird, taking place in Zanaris.  The bank chests aren't recognized by the BankLocation API,
     * plus there are some useful agility shortcuts to take. It's cleaner just to handle its logic separately. Because
     * the Bank api doesn't work here, I unfortunately cannot use many of the convenience methods from Activities.
     */
    public static Activity craftCosmicRunes() {
        Position bankChest = new Position(2386, 4458);
        Position altar = new Position(2410, 4380);

        Position agilityNorthside = new Position(2409, 4405);
        Position agilitySouthside = new Position(2409, 4398);

        return Activity.newBuilder()
                .withName("Runecrafting " + Type.COSMIC.getName())

                .addSubActivity(goToZanaris())

                // Get the talisman and ess from bank chest in NW Zanaris
                .addSubActivity(Activities.moveTo(bankChest))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 1000 * 5))

                .addSubActivity(() -> Bank.depositInventory())
                .addSubActivity(() -> Time.sleepUntil(Inventory::isEmpty, 1000 * 3))
                .addSubActivity(() -> Bank.withdraw(Type.COSMIC.getTalisman(), 1))
                .addSubActivity(() -> Time.sleepUntil(() -> Inventory.contains(Type.COSMIC.getTalisman()), 1000 * 3))
                .addSubActivity(() -> Bank.withdraw("Pure Essence", 27))
                .addSubActivity(() -> Bank.close())
                .addSubActivity(() -> Time.sleepUntil(Bank::isClosed, 1000 * 5))

                .addSubActivity(Activities.moveTo(agilityNorthside))
                .addSubActivity(() -> SceneObjects.getNearest("Jutting wall").interact("Squeeze-past"))
                .addSubActivity(() -> Time.sleep(3428, 6247))
                .addSubActivity(Activities.moveTo(altar))

                .addSubActivity(Activities.use(Type.COSMIC.getTalisman()))
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious Ruins").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(() -> !ZANARIS.contains(Players.getLocal()), 1000 * 10))

                .addSubActivity(Activities.use("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Pure Essence"), 1000 * 15))

                // Pause in case a level up dialog appears
                .addSubActivity(Activities.pauseFor(Duration.ofSeconds(2)))

                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(() -> ZANARIS.contains(Players.getLocal()), 1000 * 10))

                // Go back through the agility shortcut
                .addSubActivity(Activities.moveTo(agilitySouthside))
                .addSubActivity(() -> SceneObjects.getNearest("Jutting wall").interact("Squeeze-past"))
                .addSubActivity(() -> Time.sleep(3428, 6247))

                .build();
    }

    public static Activity craftRunes(Type type) {
        if (type == Type.COSMIC) return craftCosmicRunes();
        if (type == Type.NATURE) return craftNatureRunes();

        return Activity.newBuilder()
                .withName("Runecrafting " + type.getName())

                .addSubActivity(getEss(type))

                .addSubActivity(Activities.moveTo(type.getPosition()))
                .addSubActivity(Activities.use(type.getTalisman()))
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious Ruins").interact("Use"))

                // Altars are located North beyond the viewable world map
                .addSubActivity(() -> Time.sleepUntil(() -> Players.getLocal().getY() > 4000, 1000 * 10))

                .addSubActivity(Activities.use("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Pure Essence"), 1000 * 10))

                // Pause in case a level up dialog appears
                .addSubActivity(Activities.pauseFor(Duration.ofSeconds(2)))
                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(() -> Players.getLocal().getY() < 4000, 1000 * 10))

                .build();
    }

    private static Activity getEss(Type type) {
        return Activity.newBuilder()
                .withName("Fetching pure ess")
                .addPreReq(() -> !Inventory.contains("Pure Essence"))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(Activities.withdraw(type.getTalisman(), 1))
                .addSubActivity(Activities.withdraw("Pure Essence", 27))
                .addSubActivity(Activities.closeBank())
                .build();
    }

    public enum Type {
        AIR("air runes", "Air Talisman", AIR_ALTAR.getCenter()),
        BODY("body runes", "Body Talisman", new Position(3053, 3441)),
        COSMIC("cosmic runes", "Cosmic Talisman", new Position(2405, 4381)),
        NATURE("nature runes", "Nature talisman", new Position(2866, 3021));

        private final String name;
        private final String talisman;
        private final Position position;

        Type(String name, String talisman, Position position) {
            this.name = name;
            this.talisman = talisman;
            this.position = position;
        }

        public String getName() {
            return name;
        }

        public String getTalisman() {
            return talisman;
        }

        public Position getPosition() {
            return position;
        }
    }
}
