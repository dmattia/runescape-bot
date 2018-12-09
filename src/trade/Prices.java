package trade;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import util.common.Requests;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Prices {
    private static final String PRICE_URL = "https://rsbuddy.com/exchange/summary.json";
    private static final Gson GSON = new Gson();

    /**
     * Returns all tradeable items known in the game with their common buy/sell price info from the rsbuddy api.
     *
     * @return
     */
    private static final Set<PriceSummary> getAllItems() {
        return Requests.getJson(PRICE_URL)
                .map(JsonObject::entrySet)
                .map(Set::stream)
                .map(stream -> stream
                        .map(Map.Entry::getValue)
                        .map(jsonVal -> GSON.fromJson(jsonVal, PriceSummary.class))
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    /**
     * Experimental: Tries to find good items to trade for profit. This looks for commonly traded items
     * that are being bought for more than they are selling for.  It is yet to be seen how effective this
     * is for finding good items to buy.
     */
    public static final Set<PriceSummary> getMerchableItems() {
        return getAllItems().stream()
                .filter(priceSummary -> priceSummary.getBuyAverage() > 5)
                .filter(priceSummary -> priceSummary.getSellAverage() > 5)
                .filter(priceSummary -> priceSummary.getBuyQuantity() > 1000)
                .filter(priceSummary -> priceSummary.getSellQuantity() > 1000)
                .filter(priceSummary -> priceSummary.getBuyAverage() < 10000000)
                .filter(priceSummary -> priceSummary.getMargin() > 0)
                .sorted((itemA, itemB) -> itemB.getMargin() - itemA.getMargin())
                .limit(10)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the price summary for a single item by item id.
     */
    public static final Optional<PriceSummary> getPriceSummary(int itemId) {
        return getPriceSummary(summary -> summary.getId() == itemId);
    }

    /**
     * Returns the price summary for a single item by item name.
     */
    public static final Optional<PriceSummary> getPriceSummary(String itemName) {
        return getPriceSummary(summary -> summary.getName().toLowerCase().equals(itemName.toLowerCase()));
    }

    /**
     * Returns the price summary for a single item by some condition.
     */
    public static final Optional<PriceSummary> getPriceSummary(Predicate<PriceSummary> condition) {
        return getAllItems().stream()
                .filter(item -> condition.test(item))
                .findFirst();
    }


}
