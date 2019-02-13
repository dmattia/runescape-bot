package util;

import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.providers.RSWorld;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

class Predicates {
    static final Predicate<Item> FEATHER_PACK_PREDICATE = item -> item.getName().contains("Feather pack");

    static boolean worldIsSafe(RSWorld world) {
        return world.isMembers() &&
                !world.isSkillTotal() &&
                !world.isHighRisk() &&
                !world.isBounty() &&
                world.getPopulation() > 50 &&
                !world.isLastManStanding() &&
                !world.isSeasonDeadman() &&
                !world.isPVP() &&
                !world.isTournament();
    }

    static BooleanSupplier not(BooleanSupplier condition) {
        return () -> !condition.getAsBoolean();
    }
}
