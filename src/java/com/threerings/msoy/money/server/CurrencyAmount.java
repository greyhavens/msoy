//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Tracks a Currency and an amount.
 * TODO?: we could move this to /data and use it more frequently...
 */
public class CurrencyAmount
{
    /** The currency. */
    public Currency currency;

    /** The amount of the currency. */
    public int amount;

    /**
     * Construct a CurrencyAmount.
     */
    public CurrencyAmount (Currency currency, int amount)
    {
        this.currency = currency;
        this.amount = amount;
    }
}
