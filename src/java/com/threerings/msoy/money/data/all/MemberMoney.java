//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The amount of money a member has in their coins, bars, and bling accounts.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
public class MemberMoney implements IsSerializable
{
    public int memberId;
    public int coins;
    public int bars;
    public int bling;
    public long accCoins;
    public long accBars;
    public long accBling;

    /** Suitable for unserialization. */
    public MemberMoney ()
    {
    }

    public MemberMoney (int memberId)
    {
        this.memberId = memberId;
    }

    public MemberMoney (
        int memberId, int coins, int bars, int bling,
        long accCoins, long accBars, long accBling)
    {
        this.memberId = memberId;
        this.coins = coins;
        this.bars = bars;
        this.bling = bling;
        this.accCoins = accCoins;
        this.accBars = accBars;
        this.accBling = accBling;
    }

    /**
     * Get the current balance for the specified currency.
     * @return one of {@link #coins}, {@link #bars}, or {@link #bling}.
     */
    public int getBalance (Currency currency)
    {
        switch (currency) {
        case COINS: default: return coins;
        case BARS: return bars;
        case BLING: return bling;
        }
    }

    /**
     * Get the total accumulated value that has been added to this member's balance over the years.
     * @return one of {@link #accCoins}, {@link #accBars}, or {@link #accBling}.
     */
    public long getAccumulated (Currency currency)
    {
        switch (currency) {
        case COINS: default: return accCoins;
        case BARS: return accBars;
        case BLING: return accBling;
        }
    }
}
