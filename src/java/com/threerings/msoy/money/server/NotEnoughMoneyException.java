//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.data.all.BalanceInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.InsufficientFundsException;

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

    @Override
    public ServiceException toServiceException ()
    {
        BalanceInfo balances = new BalanceInfo();
        switch (_currency) {
        case COINS:
            balances.coins = _moneyAvailable;
            break;

        case BARS:
            balances.bars = _moneyAvailable;
            break;

        case BLING:
            balances.bling = _moneyAvailable;
            break;
        }

        return new InsufficientFundsException(_currency, balances);
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

    protected final int _moneyAvailable;
    protected final int _moneyRequested;
    protected final Currency _currency;
    protected final int _memberId;
}
