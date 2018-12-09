package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.tab.Equipment;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.House;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;

public class Runecrafting {
    private static final Area AIR_ALTAR = Area.rectangular(2982, 3288, 2988, 3294);
    private static final Area ZANARIS = Area.rectangular(2309, 4347, 2510, 4492);

    /**
     * Crafts natures safely on Karamja. Uses the general store method.
     */
    /*
    public static Activity craftNatureRunes() {
        Position generalStore = new Position(2770, 3120);
        Predicate<Item> essPredicate = item -> item.getName().equals("Pure essence") && !item.isNoted();
        BooleanSupplier isInRunes = () -> Players.getLocal().getY() > 4000;

        Activity enterPortal = Activity.newBuilder()
                .withName("Entering Portal")
                .addPreReq(() -> !isInRunes.getAsBoolean())
                .addSubActivity(Activities.moveTo(Type.NATURE.getPosition()))
                .addSubActivity(Activities.use(Type.NATURE.getTalisman()))
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious Ruins").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(isInRunes, 1000 * 10))
                .untilPreconditionsFail()
                .maximumTimes(3)
                .build();

        Activity makeRunes = Activity.newBuilder()
                .withName("Making runes")
                .addPreReq(isInRunes)
                .addPreReq(() -> Inventory.getCount(essPredicate) > 20)
                .addSubActivity(Activities.use(essPredicate))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.getCount(essPredicate) > 20, 1000 * 10))
                .build();

        Activity leavePortal = Activity.newBuilder()
                .withName("Leaving ruins")
                .addPreReq(isInRunes)
                .addPreReq(() -> Inventory.getCount(essPredicate) == 0)
                .addPreReq(() -> SceneObjects.getNearest("Portal") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(isInRunes, 1000 * 10))
                .onlyOnce()
                .build();

        Activity getEss = Activity.newBuilder()
                .withName("Fetching some essence")
                .addPreReq(() -> !isInRunes.getAsBoolean())
                .addPreReq(() -> !Inventory.isFull())
                .addSubActivity(Activities.moveTo(generalStore))
                .addSubActivity(() -> Npcs.getNearest("Jiminua").interact("Trade"))
                .addSubActivity(() -> Time.sleepUntil(() -> Shop.isOpen(), 1000 * 10))
                .addSubActivity(() -> Shop.sellTen("Rune essence"))
                .addSubActivity(() -> Shop.sellTen("Rune essence"))
                .addSubActivity(() -> Shop.sellOne("Rune essence"))
                .addSubActivity(() -> Shop.sellOne("Rune essence"))
                .addSubActivity(() -> Shop.sellOne("Rune essence"))
                .addSubActivity(() -> Shop.buyFifty("Rune essence"))
                .addSubActivity(() -> Shop.close())
                .addSubActivity(() -> Time.sleepWhile(Shop::isOpen, 2000))
                .onlyOnce()
                .build();

        Activity tmp = Activity.newBuilder()
                .addSubActivity(() -> Shop.sellTen("Rune essence"))
                .build();

        return tmp;
    }
    */
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
        //if (type == Type.NATURE) return craftNatureRunes();

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
