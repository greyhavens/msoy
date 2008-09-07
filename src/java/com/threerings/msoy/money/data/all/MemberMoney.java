//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The amount of money a member has in their coins, bars, and bling accounts.
 *
 * @Immutable
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MemberMoney implements IsSerializable
{
    public int memberId;
    public int coins;
    public int bars;
    public int bling;
    public long accBars;
    public long accCoins;
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
}
