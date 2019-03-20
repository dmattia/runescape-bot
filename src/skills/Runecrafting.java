package skills;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.api.Varps;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.*;
import org.rspeer.runetek.api.local.Health;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.movement.transportation.FairyRing;
import org.rspeer.runetek.api.scene.*;
import org.rspeer.runetek.providers.RSItemDefinition;
import org.rspeer.runetek.providers.RSSprite;
import org.rspeer.script.events.WelcomeScreen;
import org.rspeer.script.events.breaking.BreakEvent;
import util.Activities;
import util.common.Activity;
import util.common.ActivityCollector;

import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Runecrafting {
    private static final Area AIR_ALTAR = Area.rectangular(2982, 3288, 2988, 3294);
    private static final Area ZANARIS = Area.rectangular(2309, 4347, 2510, 4492);

    /**
     * Crafts natures safely on Karamja, using a home teleport to find a fairy ring
     */
    public static Activity craftNatureRunes() {
        Position nearAltar = new Position(2866, 3021);
        int DEGRADED_GIANT_POUCH_ID = 5515;

        // TODO(dmattia): Move to activities and handle if Dark Mage was not most recently used.
        Activity npcContact = Activity.newBuilder()
                .withName("Casting npc contact")
                .addPreReq(() -> Inventory.contains("Giant pouch"))
                .addPreReq(() -> Inventory.getFirst("Giant pouch").getId() == DEGRADED_GIANT_POUCH_ID)
                .addPreReq(() -> Magic.getSpellBook() == Magic.SPELLBOOK_LUNAR)
                .addPreReq(() -> Magic.canCast(Spell.Lunar.NPC_CONTACT))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.interact(Spell.Lunar.NPC_CONTACT, "Dark Mage"))
                .thenSleepUntil(Dialog::isOpen)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(Activities.stopScriptIf(() ->
                        Inventory.getFirst("Giant pouch").getId() == DEGRADED_GIANT_POUCH_ID))
                .withoutPausingBetweenActivities()
                .build();

        // TODO(dmattia): If wearing a different ring, make sure it is deposited eventually
        Activity getRing = Activity.newBuilder()
                .withName("Getting new ring of dueling")
                .addPreReq(() -> !EquipmentSlot.RING.getItemName().contains("Ring of dueling"))
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Ring of dueling(8)", 1))
                .build();

        Activity prepareBank = Activity.newBuilder()
                .withName("Preparing bank")

                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> !Inventory.contains("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll("Nature rune"))
                .addSubActivity(getRing)
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(Activities.equip("Ring of dueling(8)"))
                .addSubActivity(npcContact)

                .addSubActivity(() -> Inventory.getFirst("Giant pouch").interact("Fill"))
                .addSubActivity(() -> Inventory.getFirst("Small pouch").interact("Fill"))

                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())

                .addSubActivity(() -> Inventory.getFirst("Medium pouch").interact("Fill"))
                .addSubActivity(() -> Inventory.getFirst("Large pouch").interact("Fill"))

                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())

                .build();

        Activity goHome = Activity.newBuilder()
                .withName("Going home")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> Inventory.contains("Pure Essence"))
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").click())
                .thenSleepUntil(House::isInside)
                .tick()
                .build();

        Activity switchToLunar = Activity.newBuilder()
                .withName("Switching to lunar spellbook")
                .addPreReq(() -> Magic.getSpellBook() != Magic.SPELLBOOK_LUNAR)
                .addPreReq(House::isInside)
                .addSubActivity(() -> SceneObjects.getNearest("Lunar Altar").interact("Venerate"))
                .thenSleepUntil(Dialog::isOpen)
                .addSubActivity(() -> Dialog.process("Lunar"))
                .thenSleepUntil(() -> Magic.getSpellBook() == Magic.SPELLBOOK_LUNAR)
                .withoutPausingBetweenActivities()
                .build();

        Activity drinkFromPool = Activity.newBuilder()
                .withName("Drinking from pool for some run energy")
                .addPreReq(House::isInside)
                .addPreReq(() -> Inventory.contains("Pure Essence"))
                .addPreReq(() -> SceneObjects.getNearest("Ornate rejuvenation pool") != null)
                .addPreReq(() -> Movement.getRunEnergy() < 45)
                .addSubActivity(() -> SceneObjects.getNearest("Ornate rejuvenation pool").click())
                .thenSleepUntil(() -> Movement.getRunEnergy() == 100)
                .withoutPausingBetweenActivities()
                .build();

        Activity goNearAltar = Activity.newBuilder()
                .withName("Going to Nature altar")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> Players.getLocal().distance(nearAltar) < 500)
                .addSubActivity(Activities.moveTo(nearAltar))
                .build();

        Activity enterRuins = Activity.newBuilder()
                .withName("Entering ruins runes")
                .addPreReq(() -> Players.getLocal().distance(nearAltar) < 25)
                .addPreReq(() -> SceneObjects.getNearest("Mysterious Ruins") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious ruins").interact("Enter"))
                .thenSleepUntil(() -> Players.getLocal().distance(nearAltar) > 500)
                .build();

        Activity craftRunes = Activity.newBuilder()
                .withName("Crafting runes")
                .addPreReq(() -> Inventory.contains("Pure essence"))
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addPreReq(() -> SceneObjects.getNearest("Altar").containsAction("Craft-rune"))
                .addPreReq(() -> !House.isInside())
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Craft-rune"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .tick()
                .addSubActivity(() -> Inventory.getFirst("Giant pouch").interact("Empty"))
                .addSubActivity(() -> Inventory.getFirst("Small pouch").interact("Empty"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Craft-rune"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .tick()
                .addSubActivity(() -> Inventory.getFirst("Large pouch").interact("Empty"))
                .addSubActivity(() -> Inventory.getFirst("Medium pouch").interact("Empty"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Craft-rune"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .tick()
                .build();

        Activity goToBank = Activity.newBuilder()
                .withName("Heading to castle wars to bank")
                .addPreReq(() -> Inventory.contains("Nature rune"))
                .addPreReq(() -> !Inventory.contains("Pure essence"))
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Castle Wars"))
                .thenSleepUntil(() -> Players.getLocal().distance(nearAltar) > 100)
                .thenSleepUntil(() -> SceneObjects.getNearest("Bank chest") != null)
                .build();

        return Activity.newBuilder()
                .withName("Making nature runes")
                .addPreReq(() -> EquipmentSlot.MAINHAND.getItemName().equals("Lunar staff"))
                // TODO: Support dramen staff or elite lumbridge diary
                .addPreReq(() -> EquipmentSlot.HEAD.getItemName().equals("Nature tiara"))
                .addPreReq(() -> Inventory.contains("Rune pouch"))
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                // TODO: Add check for dark mage as npc contact option
                // TODO: Add Activity-specific break event api
                .addSubActivity(() -> BreakEvent.setCondition(() -> Players.getLocal().distance(nearAltar) < 500))
                .addSubActivity(npcContact)
                .addSubActivity(prepareBank)
                .addSubActivity(goHome)
                .addSubActivity(switchToLunar)
                .addSubActivity(drinkFromPool)
                .addSubActivity(Activities.useHouseFairyRing(FairyRing.Destination.CKR))
                .addSubActivity(goNearAltar)
                .addSubActivity(enterRuins)
                .addSubActivity(craftRunes)
                .addSubActivity(goToBank)
                .build();
    }

    /**
     * Goes to Zanaris the non Fairy Ring way, using the building in Lumbridge Swanp.
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

    public static Activity craftCosmicRunes() {
        Activity repairPouches = Activity.newBuilder()
                .withName("Repairing pouches via npc contact")

                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)

                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 601, 1000 * 15))
                .addSubActivity(() -> Bank.withdraw("Astral rune", 1))
                .tick()
                .addSubActivity(() -> Bank.withdraw("Cosmic rune", 1))
                .tick()
                .addSubActivity(() -> Bank.withdraw("Air rune", 1))
                .tick()
                .addSubActivity(() -> Bank.withdraw("Air rune", 1))
                .tick()
                .addSubActivity(() -> Bank.close())
                .addSubActivity(() -> Time.sleepUntil(Bank::isClosed, 1000 * 5))

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

        return Activity.newBuilder()
                .withName("Cosmic running")
                .addPreReq(() -> Equipment.contains("Lunar staff"))
                .addPreReq(() -> Equipment.contains("Cosmic tiara"))

                .addSubActivity(repairPouches)
                .addSubActivity(() -> {
                    IntStream.rangeClosed(1, 20)
                            .mapToObj(runNumber ->
                                    Activity.newBuilder()
                                            .withName("Cosmic running: run number " + runNumber)
                                            .addSubActivity(craftCosmicRunesSingleRun())
                                            .onlyOnce()
                                            .build()
                            )
                            .collect(new ActivityCollector())
                            .build()
                            .run();
                })
                .untilPreconditionsFail()
                .build();
    }

    private static Activity craftCosmicRunesSingleRun() {
        Position altar = new Position(2410, 4380);

        Position agilityNorthside = new Position(2409, 4405);

        Activity prepareBank = Activity.newBuilder()
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> !Inventory.contains("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 601, 1000 * 15))
                .addSubActivity(() -> Bank.depositAll("Cosmic rune"))
                .tick()
                .addSubActivity(() -> Bank.withdraw("Pure Essence", 24))
                .tick()
                .addSubActivity(() -> Bank.close())
                .addSubActivity(() -> Time.sleepUntil(Bank::isClosed, 1000 * 5))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                // TODO(dmattia): extract to private static method
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .filter(item -> !item.getName().startsWith("Rune"))
                            .map(item -> Activity.of(() -> item.interact("Fill")))
                            .collect(new ActivityCollector())
                            .build()
                            .run();
                })
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 601, 1000 * 15))
                .addSubActivity(() -> Bank.withdraw("Pure Essence", 24))
                .thenPauseFor(Duration.ofMillis(624))
                .addSubActivity(() -> Bank.close())
                .addSubActivity(() -> Time.sleepUntil(Bank::isClosed, 1000 * 5))
                .build();

        Activity restoreRunEnergy = Activity.newBuilder()
                .withName("Resoting run energy")
                .addPreReq(House::isInside)
                .addPreReq(() -> Movement.getRunEnergy() < 30)
                .addSubActivity(() -> SceneObjects.getNearest(so -> so.getName().endsWith(" pool")).click())
                .addSubActivity(() -> Time.sleepUntil(() -> Movement.getRunEnergy() == 100, 365, 1000 * 15))
                .thenPauseFor(Duration.ofMillis(500))
                .build();

        Activity goHome = Activity.newBuilder()
                .withName("Going home & handling business there")
                .addPreReq(() -> Inventory.contains("Teleport to house"))
                .addPreReq(() -> Inventory.contains("Pure Essence"))
                .addPreReq(() -> Inventory.contains("Large pouch"))
                .addPreReq(() -> !House.isInside())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> Inventory.getFirst("Teleport to house").click())
                .addSubActivity(() -> Time.sleepUntil(House::isInside, 650, 1000 * 12))
                .tick()
                .addSubActivity(restoreRunEnergy)
                .addSubActivity(() -> SceneObjects.getNearest("Fairy ring").interact("Zanaris"))
                .addSubActivity(() -> Time.sleepWhile(House::isInside, 10000))
                .build();

        Activity goToRunes = Activity.newBuilder()
                .withName("Going to Cosmic altar using agility shortcut")
                .addPreReq(() -> ZANARIS.contains(Players.getLocal()))
                .addSubActivity(Activities.moveTo(agilityNorthside))
                .addSubActivity(() -> SceneObjects.getNearest("Jutting wall").interact("Squeeze-past"))
                .addSubActivity(() -> Time.sleep(3428, 6247))
                .addSubActivity(Activities.moveTo(altar))
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious Ruins").interact("Enter"))
                .addSubActivity(() -> Time.sleepUntil(() -> !ZANARIS.contains(Players.getLocal()), 1000 * 10))
                .build();

        Activity craftRunes = Activity.newBuilder()
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addSubActivity(Activities.use("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Pure Essence"), 1000 * 15))

                // Pause in case a level up dialog appears
                .thenPauseFor(Duration.ofSeconds(2))
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .map(item -> Activity.of(() -> item.interact("Empty")))
                            .collect(new ActivityCollector())
                            .build()
                            .run();
                })
                .addSubActivity(Activities.use("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(() -> Time.sleepWhile(() -> Inventory.contains("Pure Essence"), 1000 * 15))

                // Pause in case a level up dialog appears
                .thenPauseFor(Duration.ofSeconds(2))
                .build();

        Activity goToBank = Activity.newBuilder()
                .withName("Going to bank")
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Castle Wars"))
                .thenPauseFor(Duration.ofSeconds(3))
                .build();

        Activity switchRingsIfNecessary = Activity.newBuilder()
                .withName("Exchanging used up ring of dueling for a new one")
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("(1)"))
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.unequip())
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 601, 1000 * 15))
                .addSubActivity(() -> Bank.deposit("Ring of dueling(1)", 1))
                .tick()
                .addSubActivity(() -> Bank.withdraw("Ring of dueling(8)", 1))
                .addSubActivity(() -> Time.sleepUntil(() -> Inventory.contains("Ring of dueling(8)"), 1000 * 3))
                .addSubActivity(() -> Bank.close())
                .addSubActivity(() -> Time.sleepUntil(Bank::isClosed, 1000 * 5))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> Inventory.getFirst("Ring of dueling(8)").interact("Wear"))
                .build();

        return Activity.newBuilder()
                .addPreReq(() -> Equipment.contains("Lunar Staff"))
                .addSubActivity(switchRingsIfNecessary)
                .addSubActivity(prepareBank)
                .addSubActivity(goHome)
                .addSubActivity(goToRunes)
                .addSubActivity(craftRunes)
                .addSubActivity(goToBank)
                .build();
    }

    public static Activity astralRunes() {
        Position nearAltar = new Position(2156, 3864);
        Position moonClanTeleport = new Position(2113, 3914);
        int DEGRADED_GIANT_POUCH_ID = 5515;

        // TODO(dmattia): Move to Activities
        Activity npcContact = Activity.newBuilder()
                .withName("Casting npc contact")
                .addPreReq(() -> Inventory.contains("Giant pouch"))
                .addPreReq(() -> Inventory.getFirst("Giant pouch").getId() == DEGRADED_GIANT_POUCH_ID)
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.interact(Spell.Lunar.NPC_CONTACT, "Dark Mage"))
                .thenSleepUntil(Dialog::isOpen)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .withoutPausingBetweenActivities()
                .build()
                .andThen(Activities.stopScriptIf(() -> Inventory.getFirst("Giant pouch").getId() == DEGRADED_GIANT_POUCH_ID));

        Activity getRing = Activity.newBuilder()
                .addPreReq(() -> EquipmentSlot.RING.getItem() == null)
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Ring of dueling(8)", 1))
                .build();

        Activity getStamPot = Activity.newBuilder()
                .withName("Getting stam pot")
                .addPreReq(Bank::isOpen)
                .addPreReq(() -> Movement.getRunEnergy() < 80)
                .addSubActivity(Activities.withdraw(item -> item.getName().contains("Stamina potion"), 1))
                .build();

        Activity consumeStamPot = Activity.newBuilder()
                .withName("Drinking stam pot")
                .addPreReq(() -> Inventory.contains(item -> item.getName().contains("Stamina potion")))
                .addSubActivity(() -> Inventory.getFirst(item -> item.getName().contains("Stamina potion")).click())
                .build();

        Activity getFood = Activity.newBuilder()
                .withName("Getting food to heal my hurt soul")
                .addPreReq(Bank::isOpen)
                .addPreReq(() -> Health.getCurrent() < 55)
                .addSubActivity(Activities.withdraw("Monkfish", 1))
                .build();

        Activity prepareBank = Activity.newBuilder()
                .withName("Preparing bank")

                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> !Inventory.contains("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll("Astral rune"))
                .addSubActivity(getStamPot)
                .addSubActivity(getRing)
                .addSubActivity(getFood)
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(Activities.equip("Ring of dueling(8)"))
                .addSubActivity(Activities.consumeAll("Monkfish"))
                .addSubActivity(consumeStamPot)

                .addSubActivity(() -> Inventory.getFirst("Giant pouch").interact("Fill"))
                .addSubActivity(() -> Inventory.getFirst("Small pouch").interact("Fill"))

                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll(item -> item.getName().contains("Stamina potion")))
                .addSubActivity(Activities.depositAll("Vial"))
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())

                .addSubActivity(() -> Inventory.getFirst("Medium pouch").interact("Fill"))
                .addSubActivity(() -> Inventory.getFirst("Large pouch").interact("Fill"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())

                .withoutPausingBetweenActivities()
                .build();

        Activity goNearAltar = Activity.newBuilder()
                .withName("Heading near the astral altar")
                .addPreReq(() -> Players.getLocal().distance(nearAltar) >= 25)
                .addPreReq(() -> Inventory.contains("Pure Essence"))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.MOONCLAN_TELEPORT))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .thenSleepUntil(() -> Players.getLocal().distance(moonClanTeleport) < 10)
                .addSubActivity(Activities.moveTo(nearAltar))
                .withoutPausingBetweenActivities()
                .build();

        Activity craftRunes = Activity.newBuilder()
                .withName("Crafting runes")
                .addPreReq(() -> Inventory.contains("Pure essence"))
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addPreReq(() -> Players.getLocal().distance(nearAltar) < 25)
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Craft-rune"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .thenPauseFor(Duration.ofMillis(800))
                .addSubActivity(() -> Inventory.getFirst("Giant pouch").interact("Empty"))
                .addSubActivity(() -> Inventory.getFirst("Medium pouch").interact("Empty"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Craft-rune"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .thenPauseFor(Duration.ofMillis(800))
                .addSubActivity(() -> Inventory.getFirst("Large pouch").interact("Empty"))
                .addSubActivity(() -> Inventory.getFirst("Small pouch").interact("Empty"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Craft-rune"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .thenPauseFor(Duration.ofMillis(800))
                .build();

        Activity goToBank = Activity.newBuilder()
                .withName("Heading to castle wars to bank")
                .addPreReq(() -> Inventory.contains("Astral rune"))
                .addPreReq(() -> !Inventory.contains("Pure essence"))
                .addPreReq(() -> Players.getLocal().distance(nearAltar) < 100)
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Castle Wars"))
                .thenSleepUntil(() -> Players.getLocal().distance(nearAltar) > 100)
                .addSubActivity(Activities.magicImbue())
                .build();

        return Activity.newBuilder()
                .withName("Making astral runes")
                .addPreReq(() -> EquipmentSlot.MAINHAND.getItemName().equals("Steam battlestaff"))
                .addPreReq(() -> Inventory.contains("Dust rune"))
                .addPreReq(() -> Inventory.contains("Rune pouch"))
                .addPreReq(() -> Health.getCurrent() > 20)
                .addSubActivity(npcContact)
                .addSubActivity(prepareBank)
                .addSubActivity(goNearAltar)
                .addSubActivity(craftRunes)
                .addSubActivity(goToBank)
                .build();
    }

    public static Activity steamRunes() {
        Position fireAltar = new Position(3307, 3245); // location near the fire altar ruins
        Position fireAltarInside = new Position(2578, 4845); // location inside the fire altar
        int DEGRADED_GIANT_POUCH_ID = 5515;

        // TODO(dmattia): Move to activities and handle if Dark Mage was not most recently used.
        Activity npcContact = Activity.newBuilder()
                .withName("Casting npc contact")
                .addPreReq(() -> Inventory.contains("Giant pouch"))
                .addPreReq(() -> Inventory.getFirst("Giant pouch").getId() == DEGRADED_GIANT_POUCH_ID)
                .addPreReq(() -> Magic.getSpellBook() == Magic.SPELLBOOK_LUNAR)
                .addPreReq(() -> Magic.canCast(Spell.Lunar.NPC_CONTACT))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.interact(Spell.Lunar.NPC_CONTACT, "Dark Mage"))
                .thenSleepUntil(Dialog::isOpen)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(() -> Dialog.processContinue())
                .thenSleepWhile(Dialog::isProcessing)
                .addSubActivity(Activities.stopScriptIf(() ->
                        Inventory.getFirst("Giant pouch").getId() == DEGRADED_GIANT_POUCH_ID))
                .withoutPausingBetweenActivities()
                .build();

        Activity getNecklace = Activity.newBuilder()
                .addPreReq(() -> EquipmentSlot.NECK.getItem() == null)
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Binding necklace", 1))
                .build();

        Activity getRing = Activity.newBuilder()
                .addPreReq(() -> EquipmentSlot.RING.getItem() == null)
                .addPreReq(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Ring of dueling(8)", 1))
                .build();

        Activity getStamPot = Activity.newBuilder()
                .withName("Getting stam pot")
                .addPreReq(Bank::isOpen)
                .addPreReq(() -> Movement.getRunEnergy() < 40)
                .addSubActivity(Activities.withdraw(item -> item.getName().contains("Stamina potion"), 1))
                .build();

        Activity consumeStamPot = Activity.newBuilder()
                .withName("Drinking stam pot")
                .addPreReq(() -> Inventory.contains(item -> item.getName().contains("Stamina potion")))
                .addSubActivity(() -> Inventory.getFirst(item -> item.getName().contains("Stamina potion")).click())
                .build();

        Activity equipItems = Activity.newBuilder()
                .withName("Equipping items")
                .addSubActivity(Activities.equip("Binding necklace"))
                .addSubActivity(Activities.equip("Ring of dueling(8)"))
                .build();

        Activity prepareBank = Activity.newBuilder()
                .withName("Preparing bank")
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> !Inventory.contains("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll("Steam rune"))
                .addSubActivity(getStamPot)
                .addSubActivity(getNecklace)
                .addSubActivity(getRing)
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(equipItems)
                .addSubActivity(consumeStamPot)
                // TODO(dmattia): extract to private static method
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .filter(item -> !item.getName().startsWith("Rune"))
                            .map(item -> Activity.of(() -> item.interact("Fill")))
                            .collect(new ActivityCollector())
                            .withoutPausingBetweenActivities()
                            .build()
                            .run();
                })
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll(item -> item.getName().contains("Stamina potion")))
                .addSubActivity(Activities.depositAll("Vial"))
                .addSubActivity(Activities.withdrawAll("Pure Essence"))
                .addSubActivity(Activities.closeBank())
                .withoutPausingBetweenActivities()
                .build();

        Activity goNearRuins = Activity.newBuilder()
                .withName("Heading near the fire ruins")
                .addPreReq(() -> Players.getLocal().distance(fireAltar) > 100)
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Duel Arena"))
                .thenSleepUntil(() -> Players.getLocal().distance(fireAltar) < 100)
                .thenPauseFor(Duration.ofMillis(140))
                .withoutPausingBetweenActivities()
                .build();

        Activity enterRunes = Activity.newBuilder()
                .withName("Entering ruins")
                .addPreReq(() -> SceneObjects.getNearest("Mysterious ruins") != null)
                .addPreReq(() -> Players.getLocal().distance(fireAltar) < 100)
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious ruins").interact("Enter"))
                .thenSleepUntil(() -> Players.getLocal().distance(fireAltarInside) < 100)
                .withoutPausingBetweenActivities()
                .build();

        Activity craftSteamRunes = Activity.newBuilder()
                .withName("Crafting steam runes")
                .addPreReq(() -> Players.getLocal().distance(fireAltarInside) < 100)
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addPreReq(() -> Inventory.contains("Pure essence"))
                .addPreReq(() -> Inventory.getCount(true, "Water rune") > 45)
                .thenPauseFor(Duration.ofMillis(350))
                .addSubActivity(() -> Inventory.getFirst("Water rune").interact("Use"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.MAGIC_IMBUE))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .filter(item -> !item.getName().startsWith("Rune"))
                            .map(item -> Activity.of(() -> item.interact("Empty")))
                            .collect(new ActivityCollector())
                            .build()
                            .run();
                })
                .addSubActivity(() -> Inventory.getFirst("Water rune").interact("Use"))
                .addSubActivity(() -> SceneObjects.getNearest("Altar").interact("Use"))
                .thenSleepUntil(() -> !Inventory.contains("Pure essence"))
                .build();

        Activity goToBank = Activity.newBuilder()
                .withName("Heading to castle wars to bank")
                .addPreReq(() -> Inventory.getCount(true, "Steam rune") > 22) // ensure pouches were used
                .addPreReq(() -> Players.getLocal().distance(fireAltarInside) < 100)
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Castle Wars"))
                .thenSleepUntil(() -> Players.getLocal().distance(fireAltarInside) > 100)
                .thenPauseFor(Duration.ofMillis(210))
                .withoutPausingBetweenActivities()
                .build();

        return Activity.newBuilder()
                .withName("Making steam runes")
                .addPreReq(() -> EquipmentSlot.HEAD.getItemName().equals("Fire tiara"))
                .addPreReq(() -> EquipmentSlot.MAINHAND.getItemName().equals("Steam battlestaff"))
                .addPreReq(() -> Inventory.contains("Rune pouch"))
                .addPreReq(() -> Inventory.contains("Water rune"))
                .addSubActivity(Activities.toggleRun())
                .addSubActivity(npcContact)
                .addSubActivity(prepareBank)
                .addSubActivity(goNearRuins)
                .addSubActivity(enterRunes)
                .addSubActivity(craftSteamRunes)
                .addSubActivity(goToBank)
                //.withoutPausingBetweenActivities()
                .build();
    }

    public static Activity mudRunes() {
        Position balloonPos = new Position(2456, 3102);
        Position earthAltar = new Position(2657, 4830);
        int magicImbueVarpIndex = 5438;

        Activity npcContact = Activity.newBuilder()
                .withName("Casting npc contact")
                .addPreReq(() -> EquipmentSlot.NECK.getItem() == null)
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.interact(Spell.Lunar.NPC_CONTACT, "Dark Mage"))
                .thenSleepUntil(Dialog::isOpen)
                .addSubActivity(() -> Dialog.processContinue())
                .addSubActivity(() -> Dialog.processContinue())
                .addSubActivity(() -> Dialog.processContinue())

                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Binding necklace", 1))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> Inventory.getFirst("Binding necklace").interact("Wear"))
                .build();

        Activity switchRingsIfNecessary = Activity.newBuilder()
                .withName("Exchanging used up ring of dueling for a new one")
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("(1)"))
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.unequip())
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll("Ring of dueling(1)"))
                .addSubActivity(Activities.withdraw("Ring of dueling(8)", 1))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .addSubActivity(() -> Inventory.getFirst("Ring of dueling(8)").interact("Wear"))
                .build();

        Activity prepareBank = Activity.newBuilder()
                .withName("Preparing bank")
                .addPreReq(() -> SceneObjects.getNearest("Bank chest") != null)
                .addPreReq(() -> !Inventory.contains("Pure Essence"))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.depositAll("Mud rune"))
                .addSubActivity(Activities.withdraw("Pure Essence", 22))
                .addSubActivity(Activities.withdraw("Willow logs", 1))
                .addSubActivity(Activities.closeBank())
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                // TODO(dmattia): extract to private static method
                .addSubActivity(() -> {
                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .filter(item -> !item.getName().startsWith("Rune"))
                            .map(item -> Activity.of(() -> item.interact("Fill")))
                            .collect(new ActivityCollector())
                            .build()
                            .run();
                })
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .thenSleepUntil(Bank::isOpen)
                .addSubActivity(Activities.withdraw("Pure Essence", 22))
                .addSubActivity(Activities.closeBank())
                //.withoutPausingBetweenActivities()
                .build();

        Activity handleFlyInterface = Activity.newBuilder()
                .withName("Clicking Varrock on the flying map interface")
                .addPreReq(() -> Interfaces.getComponent(469, 18) != null)
                .addSubActivity(() -> Interfaces.getComponent(469, 18).click())
                .build();

        Activity flyAction = Activity.newBuilder()
                .withName("Talking to Assistant Marrow to fly the balloon")
                .addPreReq(() -> Npcs.getNearest("Assistant Marrow") != null)
                .addSubActivity(() -> Npcs.getNearest("Assistant Marrow").interact("Fly"))
                .thenSleepUntil(() -> Interfaces.getComponent(469, 18) != null)
                .addSubActivity(handleFlyInterface)
                .build();

        Activity flyBalloon = Activity.newBuilder()
                .withName("Going to the earth altar")
                .addPreReq(() -> Inventory.contains("Pure essence"))
                .addPreReq(() -> Players.getLocal().distance(balloonPos) < 100)
                .addSubActivity(Activities.moveTo(balloonPos))
                .addSubActivity(flyAction)
                .thenSleepWhile(() -> Players.getLocal().distance(balloonPos) < 100)
                .thenPauseFor(Duration.ofMillis(1000))
                .build();

        Activity enterRunes = Activity.newBuilder()
                .withName("Entering runes, casting magic imbue along the way")
                .addPreReq(() -> SceneObjects.getNearest("Mysterious ruins") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Mysterious ruins").interact("Enter"))
                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                .addSubActivity(() -> Magic.cast(Spell.Lunar.MAGIC_IMBUE))
                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                .thenSleepUntil(() -> Players.getLocal().distance(earthAltar) < 100)
                .build();

        Activity craftEarthRunes = Activity.newBuilder()
                .withName("Crafting runes")
                .addPreReq(() -> Players.getLocal().distance(earthAltar) < 100)
                .addPreReq(() -> SceneObjects.getNearest("Altar") != null)
                .addPreReq(() -> Inventory.contains("Pure essence"))
                .addSubActivity(() -> {
                    if (Varps.getBitValue(magicImbueVarpIndex) == 0) {
                        Activity.newBuilder()
                                .addSubActivity(Activities.switchToTab(Tab.MAGIC))
                                .addSubActivity(() -> Magic.cast(Spell.Lunar.MAGIC_IMBUE))
                                .addSubActivity(Activities.switchToTab(Tab.INVENTORY))
                                .build()
                                .run();
                    }

                    Inventory.getFirst("Water rune").interact("Use");
                    Time.sleep(80, 140);
                    SceneObjects.getNearest("Altar").interact("Use");
                    boolean success = Time.sleepUntil(() -> !Inventory.contains("Pure essence"), 60, 10 * 1000);
                    if (!success) return;

                    Stream.of(Inventory.getItems(item -> item.getName().contains("pouch")))
                            .filter(item -> !item.getName().startsWith("Rune"))
                            .map(item -> Activity.of(() -> item.interact("Empty")))
                            .collect(new ActivityCollector())
                            .build()
                            .run();

                    Inventory.getFirst("Water rune").interact("Use");
                    Time.sleep(80, 140);
                    SceneObjects.getNearest("Altar").interact("Use");
                    Time.sleepUntil(() -> Inventory.getCount(true, "Mud rune") > 22, 60, 5000);
                })
                .build();

        Activity goToBank = Activity.newBuilder()
                .withName("Heading to castle wars")
                .addPreReq(() -> Inventory.getCount(true, "Mud rune") > 22) // ensure pouches were used
                .addPreReq(() -> Players.getLocal().distance(earthAltar) < 100)
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addSubActivity(Activities.switchToTab(Tab.EQUIPMENT))
                .addSubActivity(() -> EquipmentSlot.RING.interact("Castle Wars"))
                .thenSleepUntil(() -> Players.getLocal().distance(earthAltar) > 100)
                .build();

        return Activity.newBuilder()
                .withName("Making mud runes")
                .addPreReq(() -> EquipmentSlot.HEAD.getItemName().equals("Earth tiara"))
                .addPreReq(() -> EquipmentSlot.MAINHAND.getItemName().equals("Steam battlestaff"))
                .addPreReq(() -> EquipmentSlot.RING.getItemName().contains("dueling"))
                .addPreReq(() -> Inventory.contains("Rune pouch"))
                .addSubActivity(Activities.toggleRun())
                .addSubActivity(npcContact)
                .addSubActivity(switchRingsIfNecessary)
                .addSubActivity(prepareBank)
                .addSubActivity(flyBalloon)
                .addSubActivity(enterRunes)
                .addSubActivity(craftEarthRunes)
                .addSubActivity(goToBank)
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
                .thenPauseFor(Duration.ofSeconds(2))
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
