//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

/**
 * A base class for purchase results.
 */
public class PurchaseResult<T>
    implements IsSerializable
{
    /** Suitable for unserialization. */
    public PurchaseResult ()
    {
    }

    /** The minimum required fields. */
    public PurchaseResult (T ware, BalanceInfo balances, PriceQuote quote)
    {
        this.ware = ware;
        this.balances = balances;
        this.quote = quote;
    }

    /** Any updated balances. */
    public BalanceInfo balances;

    /** Another price quote if they wish to buy again. */
    public PriceQuote quote;

    /** The percentage of the purchase price the charity received. Optional. */
    public float charityPercentage;

    /** The name of the charity that received the donation. Optional. */
    public MemberName charity;

    /** The ware that was created. */
    public T ware;
}
