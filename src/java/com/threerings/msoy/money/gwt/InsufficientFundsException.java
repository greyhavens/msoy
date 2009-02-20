//
// $Id$

package com.threerings.msoy.money.gwt;

import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.data.all.BalanceInfo;
import com.threerings.msoy.money.data.all.Currency;

/**
 * An exception thrown when the user tries to perform an operation for which they don't
 * have the funds.
 */
public class InsufficientFundsException extends ServiceException
{
    public static String E_INSUFFICIENT_FUNDS = "e.insufficient_funds";

    public InsufficientFundsException (Currency currency, BalanceInfo balances)
    {
        super(E_INSUFFICIENT_FUNDS);
        _currency = currency;
        _balances = balances;
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
     * Get the updated applicable balances.
     */
    public BalanceInfo getBalances ()
    {
        return _balances;
    }

    protected Currency _currency;
    protected BalanceInfo _balances;
}
