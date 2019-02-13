package skills;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.runetek.api.component.Interfaces;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.component.tab.Skills;
import org.rspeer.runetek.api.input.Keyboard;
import org.rspeer.runetek.api.movement.position.Position;
import org.rspeer.runetek.api.scene.House;
import org.rspeer.runetek.api.scene.Npcs;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import util.Activities;
import util.common.Activity;

import java.time.Duration;

public class Construction {
    private static final Position GUILD_BANK = new Position(1592, 3476);

    private static final Position GUILD_SAW_MILL = new Position(1624, 3500);
    private static final Position VARROCK_SAW_MILL = new Position(3302, 3489);

    private static final Position PHIALS = new Position(2950, 3213);

    /**
     * Wish List :)
     *
     * Layout: https://www.youtube.com/watch?v=M6K0qySeRQU
     *
     * Jewelery Box:
     O   Ornate: 83 + 8 http://oldschoolrunescape.wikia.com/wiki/Ornate_jewellery_box
     *
     * Portals:
     X   Camelot: 45 mage
     O   Ardougne: 51 mage + Plague City
     O   Watchtower/Yanille: 58 mage + Watchtower
     *
     * Pools:
     O   hitpoints: 82 + 8
     */


    /**
     * TODO(dmattia): Finish adding servant support
     * Activity callServant = Activity.newBuilder()
     * .addSubActivity(() -> Tabs.open(Tab.OPTIONS))
     * .addSubActivity(() -> Time.sleepUntil(() -> Tabs.isOpen(Tab.OPTIONS), 1000 * 2))
     * <p>
     * // Once Api is fixed: .addSubActivity(() -> HouseOptions.open())
     * .addSubActivity(() -> Interfaces.getComponent(261, 98).interact("View House Options"))
     * <p>
     * .addSubActivity(() -> Time.sleepUntil(HouseOptions::isOpen, 1000 * 2))
     * .addSubActivity(() -> HouseOptions.callButler())
     * <p>
     * .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000))
     * .addSubActivity(() -> Dialog.process(0))
     * .build();
     * <p>
     * Activity acceptServantPlanks = Activity.newBuilder()
     * .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 888, 1000 * 13))
     * .addSubActivity(() -> Dialog.processContinue())
     * .onlyOnce()
     * .build();
     */

    public static Activity makeOakLarder() {
        Activity destroyLarder = Activity.newBuilder()
                .withName("Destroying a larder mwahaha")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Larder") != null)
                .addSubActivity(() -> Activities.moveTo(SceneObjects.getNearest("Larder").getPosition()).run())
                .addSubActivity(() -> SceneObjects.getNearest("Larder").interact("Remove"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 222, 1000 * 5))
                .addSubActivity(() -> Dialog.process("Yes"))
                .addSubActivity(() -> Time.sleepWhile(Dialog::isOpen, 222, 1000 * 5))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(1200)))
                .build();

        Activity makeLarder = Activity.newBuilder()
                .withName("Making a single larder")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Larder Space") != null)
                .addPreReq(() -> Inventory.getCount("Oak Plank") >= 8)
                .addSubActivity(() -> Activities.moveTo(SceneObjects.getNearest("Larder Space").getPosition()).run())
                .addSubActivity(() -> SceneObjects.getNearest("Larder Space").interact("Build"))
                .addSubActivity(() -> Time.sleepUntil(() -> Interfaces.isOpen(458), 444, 1000 * 10))
                .addSubActivity(() -> Interfaces.getComponent(458, 5).interact("Build"))
                .addSubActivity(() -> Time.sleepUntil(() -> !Players.getLocal().isAnimating(), 222, 1000 * 5))
                .onlyOnce()
                .build();

        Activity makeLarders = Activity.newBuilder()
                .withName("Making all larders possible")
                .addPreReq(House::isInside)
                .addPreReq(() -> Inventory.getCount("Oak Plank") >= 8)
                .addSubActivity(destroyLarder)
                .addSubActivity(makeLarder)
                .untilPreconditionsFail()
                .build();

        Activity unnotePlanks = Activity.newBuilder()
                .withName("Unnoting planks")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> Players.getLocal().distance(PHIALS) < 100)
                .addPreReq(() -> !Inventory.isFull())
                .addSubActivity(Activities.moveTo(PHIALS))
                .addSubActivity(Activities.use("Oak Plank"))
                .addSubActivity(() -> Npcs.getNearest("Phials").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Dialog::isOpen, 1000 * 3))
                .addSubActivity(() -> Activities.pauseFor(Duration.ofSeconds(1)))
                .addSubActivity(() -> Dialog.process(2))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(429)))
                .build();

        Activity enterHouse = Activity.newBuilder()
                .withName("Entering house")
                .addPreReq(() -> !House.isInside())
                .addPreReq(() -> SceneObjects.getNearest("Portal") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Build mode"))
                .addSubActivity(() -> Time.sleepUntil(House::isInside, 1211, 1000 * 10))
                .untilPreconditionsFail()
                .build();

        Activity leaveHouse = Activity.newBuilder()
                .withName("Leaving house")
                .addPreReq(House::isInside)
                .addPreReq(() -> SceneObjects.getNearest("Portal") != null)
                .addSubActivity(() -> SceneObjects.getNearest("Portal").interact("Enter"))
                .addSubActivity(() -> Time.sleepWhile(House::isInside, 1211, 1000 * 10))
                .build();

        return Activity.newBuilder()
                .withName("Oak Larder Activity")
                .addPreReq(() -> Skills.getCurrentLevel(Skill.CONSTRUCTION) >= 33)
                .addPreReq(() -> Inventory.getCount(true, "Coins") > 120) // Unnoting fee for Phials
                .addPreReq(() -> Inventory.getCount(true, "Oak Plank") > 24)
                .addSubActivity(makeLarders)
                .addSubActivity(leaveHouse)
                .addSubActivity(unnotePlanks)
                .addSubActivity(enterHouse)
                .build();
    }

    /**
     * 244k per hour for WC, not bad :)2
     */
    public static Activity cutAndMillOaks() {
        Activity bank = Activity.newBuilder()
                .withName("Banking oak planks")
                .addPreReq(() -> Inventory.contains("Oak plank"))
                .addSubActivity(Activities.moveTo(GUILD_BANK))
                .addSubActivity(() -> SceneObjects.getNearest("Bank chest").interact("Use"))
                .addSubActivity(() -> Time.sleepUntil(Bank::isOpen, 1000 * 4))
                .addSubActivity(Activities.depositAll("Oak plank"))
                .addSubActivity(Activities.closeBank())
                .onlyOnce()
                .build();

        return Activity.newBuilder()
                .withName("Making Oak planks in Woodcut guild")
                .addSubActivity(bank)
                .addSubActivity(Woodcutting.cut(Woodcutting.Tree.OAK))
                .addSubActivity(Activities.moveTo(GUILD_SAW_MILL))
                .addSubActivity(() -> Npcs.getNearest("Sawmill operator").interact("Buy-Plank"))
                .addSubActivity(() -> Time.sleepUntil(() -> Interfaces.isOpen(403), 1000 * 3))
                .addSubActivity(makePlanks())
                .build();
    }

    public static Activity makeSawMillRun() {
        return Activity.newBuilder()
                .withName("Varrock Sawmill running")
                .addSubActivity(Activities.wearGraceful())
                .addSubActivity(getLogs())
                .addSubActivity(Activities.moveTo(VARROCK_SAW_MILL))
                .addSubActivity(() -> Npcs.getNearest("Sawmill operator").interact("Buy-Plank"))
                .addSubActivity(() -> Time.sleepUntil(() -> Interfaces.isOpen(403), 1000 * 3))
                .addSubActivity(makePlanks())
                .onlyOnce()
                .build();
    }

    private static Activity makePlanks() {
        return Activity.newBuilder()
                .withName("Making Planks from interface")
                .addPreReq(() -> Interfaces.isOpen(403))
                .addSubActivity(() -> Interfaces.getComponent(403, 108).interact(action -> true))
                .addSubActivity(Activities.pauseFor(Duration.ofMillis(1500)))
                .addSubActivity(() -> Keyboard.sendText("27"))
                .addSubActivity(() -> Keyboard.pressEnter())
                .addSubActivity(() -> Time.sleepUntil(() -> !Interfaces.isOpen(403), 1000 * 3))
                .onlyOnce()
                .build();
    }

    private static Activity getLogs() {
        return Activity.newBuilder()
                .withName("Getting Oak logs from bank")
                .addPreReq(() -> !Inventory.contains("Oak logs"))
                .addSubActivity(Activities.depositInventory())
                .addSubActivity(() -> Bank.withdrawAll("coins"))
                .addSubActivity(Activities.withdraw("Oak logs", 27))
                .addSubActivity(Activities.closeBank())
                .build();
    }
}
