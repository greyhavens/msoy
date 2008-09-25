//
// $Id$

package com.threerings.msoy.money.server;

/**
 * Indicates that the user attempted to cash out bling when they've already requested a cash out
 * previously.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class AlreadyCashedOutException extends MoneyException
{
    /** The member whose bling was cashed out. */
    public final int memberId;
    
    /** The amount of bling previously cashed out. */
    public final int cashedOutBling;
    
    public AlreadyCashedOutException (int memberId, int cashedOutBling)
    {
        super("Member " + memberId + " has already requested a bling cash out of " + 
            cashedOutBling);
        this.memberId = memberId;
        this.cashedOutBling = cashedOutBling;
    }
    
    public AlreadyCashedOutException (int memberId, int cashedOutBling, Throwable cause)
    {
        super("Member " + memberId + " has already requested a bling cash out of " + 
            cashedOutBling, cause);
        this.memberId = memberId;
        this.cashedOutBling = cashedOutBling;
    }
}
