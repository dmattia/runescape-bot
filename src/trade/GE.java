package trade;

import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.GrandExchange;
import org.rspeer.runetek.api.scene.Npcs;
import util.Activities;
import util.Locations;
import util.common.Activity;
import org.rspeer.ui.Log;

/**
 * Utilities related to the Grand Exchange.  This uses the rsbuddy api to determine prices for buying/selling,
 * and will not use the standard GE prices unless there are no instances of the item being sold on the GE already,
 * in which case the rsbuddy api has no recommendation.
 */
public class GE {
    /**
     * Starts selling all of an item from an inventory by id.  This activity only posts the item to the GE,
     * it does not wait for it to complete selling.
     */
    public static Activity sellAll(int itemId) {
        return Activity.newBuilder()
                .addPreReq(() -> Prices.getPriceSummary(itemId).isPresent())
                .addSubActivity(Activities.moveTo(Locations.GRAND_EXCHANGE))
                .addSubActivity(() -> Npcs.getNearest("Grand Exchange Clerk").interact("Exchange"))
                .addSubActivity(() -> Time.sleepUntil(GrandExchange::isOpen, 1000 * 8))
                .addSubActivity(() -> {
                    PriceSummary priceInfo = Prices.getPriceSummary(itemId).get();
                    Log.info("Selling some " + priceInfo.getName());
                })
                .onlyOnce()
                .build();
    }
}
