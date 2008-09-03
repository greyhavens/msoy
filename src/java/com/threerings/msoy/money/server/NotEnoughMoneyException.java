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
        int memberId, MoneyType type, int amountDesired, int amountAvailable)
    {
        super("An attempt to secure " + amountDesired + " currency of type " + type
            + " from member ID " + memberId + " failed because only " + amountAvailable
            + " is available.");
        _memberId = memberId;
        _type = type;
        _moneyRequested = amountDesired;
        _moneyAvailable = amountAvailable;
    }

    public NotEnoughMoneyException (
        int memberId, MoneyType type, int amountDesired, int amountAvailable, Throwable cause)
    {
        this(memberId, type, amountDesired, amountAvailable);
        initCause(cause);
    }

    public int getMemberId ()
    {
        return _memberId;
    }

    public int getMoneyAvailable ()
    {
        return _moneyAvailable;
    }

    public int getMoneyRequested ()
    {
        return _moneyRequested;
    }

    public MoneyType getType ()
    {
        return _type;
    }

    private final int _moneyAvailable;
    private final int _moneyRequested;
    private final MoneyType _type;
    private final int _memberId;
}
