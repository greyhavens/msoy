//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.HashMap;
import java.util.Map;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.money.data.all.Currency;

/**
 * Utility routines relating to item pricing.
 */
public class ItemPrices
{
    /**
     * Returns the minimum pricing for an item of the specified type with the specified rating.
     */
    public static int getMinimumPrice (Currency currency, byte itemType, byte rating)
    {
        switch (currency) {
        case BARS:
            return 1;
        case COINS:
            rating = (byte)Math.min(4, Math.max(0, rating-1));
            int[] prices = MIN_PRICES.get(itemType);
            return (prices == null) ? DEFAULT_MIN_PRICE : prices[rating];
        default:
            throw new IllegalArgumentException("Can't compute minimum price for " + currency + ".");
        }
    }

    protected static final int DEFAULT_MIN_PRICE = 10;

    protected static final Map<Byte, int[]> MIN_PRICES = new HashMap<Byte, int[]>();
    static {
        MIN_PRICES.put(Item.AVATAR, new int[] { 100, 200, 500, 1500, 5000 });
        MIN_PRICES.put(Item.FURNITURE, new int[] { 10, 50, 100, 500, 1000 });
        MIN_PRICES.put(Item.DECOR, new int[] { 20, 100, 200, 1000, 2000 });
        MIN_PRICES.put(Item.TOY, new int[] { 200, 300, 700, 2500, 5000 });
        MIN_PRICES.put(Item.PET, new int[] { 100, 200, 500, 1500, 5000 });
        MIN_PRICES.put(Item.AVATAR, new int[] { 100, 200, 500, 1500, 5000 });
        MIN_PRICES.put(Item.GAME, new int[] { 200, 300, 700, 2500, 5000 });
        MIN_PRICES.put(Item.PHOTO, new int[] { 10, 50, 100, 500, 1000 });
        MIN_PRICES.put(Item.VIDEO, new int[] { 10, 50, 100, 500, 1000 });
    }
}
