//
// $Id$

package com.threerings.msoy.money.gwt;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.money.data.all.BalanceInfo;
import com.threerings.msoy.money.data.all.Currency;

/**
 * An exception thrown when the user tries to perform an operation for which they don't
 * have the funds.
 */
public class InsufficientFundsException extends ServiceException
{
    public static String E_INSUFFICIENT_FUNDS = "e.insufficient_funds";

    public InsufficientFundsException (Currency currency, int balance)
    {
        super(E_INSUFFICIENT_FUNDS);
        _currency = currency;
        _balance = balance;
    }

    /** Suitable for unserialization. */
    public InsufficientFundsException ()
    {
    }

    /**
     * Returns the currency in which we are insufficient.
     */
    public Currency getCurrency ()
    {
        return _currency;
    }

    /**
     * Returns the user's current balance in the currency in question.
     */
    public int getBalance ()
    {
        return _balance;
    }

    /**
     * Get the updated applicable balances.
     */
    public BalanceInfo getBalances ()
    {
        BalanceInfo info = new BalanceInfo();
        switch (_currency) {
        case COINS:
            info.coins = _balance;
            break;
        case BARS:
            info.bars = _balance;
            break;
        case BLING:
            info.bling = _balance;
            break;
        }
        return info;
    }

    protected Currency _currency;
    protected int _balance;
}
