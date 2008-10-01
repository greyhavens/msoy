//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information about a user's current bling status.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class BlingInfo
    implements IsSerializable
{
    /** An amount of centibling. */
    public int bling;

    /** The worth in US pennies. */
    public int blingWorth;
    
    /** Information about the cash out request currently in progress, or null if no request is 
     * active. */
    public CashOutInfo cashOut;
    
    public BlingInfo (int bling, int blingWorth, CashOutInfo cashOut)
    {
        this.bling = bling;
        this.blingWorth = blingWorth;
        this.cashOut = cashOut;
    }
    
    /** For serialization purposes. */
    public BlingInfo () { }
}
