//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.MoneyType;

/**
 * Occurs when a member attempts to use some amount of money they do not have.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotEnoughMoneyException extends MoneyException
{
    public NotEnoughMoneyException (
        final double moneyAvailable, final double moneyRequested, final MoneyType type,
        final int memberId)
    {
        super("An attempt to secure " + moneyRequested + " currency of type " + type
            + " from member ID " + memberId + " failed because only " + moneyAvailable
            + " is available.");
        _moneyAvailable = moneyAvailable;
        _moneyRequested = moneyRequested;
        _type = type;
        _memberId = memberId;
    }

    public NotEnoughMoneyException (
        final double moneyAvailable, final double moneyRequested, final MoneyType type,
        final int memberId, final Throwable cause)
    {
        super("An attempt to secure " + moneyRequested + " currency of type " + type
            + " from member ID " + memberId + " failed because only " + moneyAvailable
            + " is available.", cause);
        _moneyAvailable = moneyAvailable;
        _moneyRequested = moneyRequested;
        _type = type;
        _memberId = memberId;
    }

    public int getMemberId ()
    {
        return _memberId;
    }

    public double getMoneyAvailable ()
    {
        return _moneyAvailable;
    }

    public double getMoneyRequested ()
    {
        return _moneyRequested;
    }

    public MoneyType getType ()
    {
        return _type;
    }

    private final double _moneyAvailable;
    private final double _moneyRequested;
    private final MoneyType _type;
    private final int _memberId;
}
