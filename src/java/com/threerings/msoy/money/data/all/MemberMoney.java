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
    public MemberMoney (final int memberId)
    {
        this(memberId, 0, 0, 0.0, 0, 0, 0.0);
    }
    
    public MemberMoney (
        final int memberId, final int coins, final int bars, final double bling,
        final long accCoins, final long accBars, final double accBling)
    {
        _memberId = memberId;
        _coins = coins;
        _bars = bars;
        _bling = bling;
        _accCoins = accCoins;
        _accBars = accBars;
        _accBling = accBling;
    }

    public int getBars ()
    {
        return _bars;
    }

    public int getCoins ()
    {
        return _coins;
    }

    public double getBling ()
    {
        return _bling;
    }

    public int getMemberId ()
    {
        return _memberId;
    }

    public long getAccBars ()
    {
        return _accBars;
    }

    public long getAccCoins ()
    {
        return _accCoins;
    }

    public double getAccBling ()
    {
        return _accBling;
    }

    private final long _accBars;
    private final long _accCoins;
    private final double _accBling;
    private final int _bars;
    private final int _coins;
    private final double _bling;
    private final int _memberId;
}
