//
// $Id$

package com.threerings.msoy.money.server;

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
}
