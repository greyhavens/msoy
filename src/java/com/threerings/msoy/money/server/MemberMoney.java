//
// $Id$

package com.threerings.msoy.money.server;

import net.jcip.annotations.Immutable;

/**
 * The amount of money a member has in their coins, bars, and bling accounts.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Immutable
public class MemberMoney
{
    public MemberMoney (final int memberId)
    {
        this(memberId, 0, 0, 0.0, 0, 0, 0.0);
    }
    
    public MemberMoney (final int memberId, final int coins, final int bars, final double bling,
        final long accCoins, final long accBars, final double accBling)
    {
        this.memberId = memberId;
        this.coins = coins;
        this.bars = bars;
        this.bling = bling;
        this.accCoins = accCoins;
        this.accBars = accBars;
        this.accBling = accBling;
    }

    public int getBars ()
    {
        return bars;
    }

    public int getCoins ()
    {
        return coins;
    }

    public double getBling ()
    {
        return bling;
    }

    public int getMemberId ()
    {
        return memberId;
    }

    public long getAccBars ()
    {
        return accBars;
    }

    public long getAccCoins ()
    {
        return accCoins;
    }

    public double getAccBling ()
    {
        return accBling;
    }

    private final long accBars;
    private final long accCoins;
    private final double accBling;
    private final int bars;
    private final int coins;
    private final double bling;
    private final int memberId;
}
