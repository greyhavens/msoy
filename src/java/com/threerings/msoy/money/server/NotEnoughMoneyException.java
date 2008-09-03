//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Occurs when a member attempts to use some amount of money they do not have.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotEnoughMoneyException extends MoneyException
{
    public NotEnoughMoneyException (
        int memberId, Currency currency, int amountDesired, int amountAvailable)
    {
        super("An attempt to secure " + amountDesired + " " + currency
            + " from member ID " + memberId + " failed because only " + amountAvailable
            + " is available.");
        _memberId = memberId;
        _currency = currency;
        _moneyRequested = amountDesired;
        _moneyAvailable = amountAvailable;
    }

    public NotEnoughMoneyException (
        int memberId, Currency currency, int amountDesired, int amountAvailable, Throwable cause)
    {
        this(memberId, currency, amountDesired, amountAvailable);
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

    public Currency getCurrency ()
    {
        return _currency;
    }

    private final int _moneyAvailable;
    private final int _moneyRequested;
    private final Currency _currency;
    private final int _memberId;
}
