package trade;

import com.google.gson.annotations.SerializedName;

/**
 * Information about a single in game item, including the recommended buy/sell pricing info on the Grand Exchange
 * provided by Rsbuddy's api.
 */
public class PriceSummary {
    private int id;
    private String name;

    @SerializedName("buy_average")
    private int buyAverage;

    @SerializedName("sell_average")
    private int sellAverage;

    @SerializedName("sell_quantity")
    private int sellQuantity;

    @SerializedName("buy_quantity")
    private int buyQuantity;

    @Override
    public String toString() {
        return "PriceSummary{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", buyAverage=" + buyAverage +
                ", sellAverage=" + sellAverage +
                ", sellQuantity=" + sellQuantity +
                ", buyQuantity=" + buyQuantity +
                '}';
    }

    public int getMargin() {
        return 100 * sellAverage / buyAverage;
        //return sellAverage - buyAverage;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getBuyAverage() {
        return buyAverage;
    }

    public int getSellAverage() {
        return sellAverage;
    }

    public int getSellQuantity() {
        return sellQuantity;
    }

    public int getBuyQuantity() {
        return buyQuantity;
    }
}
