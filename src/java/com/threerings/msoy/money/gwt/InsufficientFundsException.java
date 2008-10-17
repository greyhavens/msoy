//
// $Id$

package com.threerings.msoy.money.gwt;

import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.data.all.BalanceInfo;

/**
 * An exception thrown when the user tries to perform an operation for which they don't
 * have the funds.
 */
public class InsufficientFundsException extends ServiceException
{
    public static String E_INSUFFICIENT_FUNDS = "e.insufficient_funds";

    public InsufficientFundsException (BalanceInfo balances)
    {
        super(E_INSUFFICIENT_FUNDS);
        _balances = balances;
    }

    /** Suitable for unserialization. */
    public InsufficientFundsException ()
    {
    }

    /**
     * Get the updated applicable balances.
     */
    public BalanceInfo getBalances ()
    {
        return _balances;
    }

    protected BalanceInfo _balances;
}
