//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.data.MoneyCodes;

public class BelowMinimumBlingException extends MoneyException
{
    public final int memberId;
    public final int amountRequested;
    public final int minimumAmount;

    public BelowMinimumBlingException (int memberId, int amountRequested, int minimumAmount)
    {
        super("The amount of bling requested, " + amountRequested +
            ", is below the minimum amount allowed: " + minimumAmount);
        this.memberId = memberId;
        this.amountRequested = amountRequested;
        this.minimumAmount = minimumAmount;
    }

    public BelowMinimumBlingException (int memberId, int amountRequested, int minimumAmount,
        Throwable cause)
    {
        super("The amount of bling requested, " + amountRequested +
            ", is below the minimum amount allowed: " + minimumAmount, cause);
        this.memberId = memberId;
        this.amountRequested = amountRequested;
        this.minimumAmount = minimumAmount;
    }

    @Override
    public ServiceException toServiceException ()
    {
        return new ServiceException(MoneyCodes.E_BELOW_MINIMUM_BLING);
    }
}
