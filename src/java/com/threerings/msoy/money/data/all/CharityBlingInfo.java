//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information about the bling in a charity's account, so it can be cashed out.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class CharityBlingInfo
    implements IsSerializable
{
    /** Member ID of the charity. */
    public /* final */ int memberId;
    
    /** Charity's name. */
    public /* final */ String displayName;
    
    /** Charity's email address. */
    public /* final */ String emailAddress;
    
    /** The amount of bling the charity currently has. */
    public /* final */ int blingAmount;
    
    /** The worth of the bling in USD cents the user has requested to cash out. */
    public /* final */ int blingWorth;

    /** Whether or not this is a core charity. */
    public /* final */ boolean core;
    
    public CharityBlingInfo (int memberId, String displayName, String emailAddress,
            int blingAmount, int blingWorth, boolean core)
    {
        this.memberId = memberId;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.blingAmount = blingAmount;
        this.blingWorth = blingWorth;
        this.core = core;
    }

    public CharityBlingInfo () { /* for serialization purposes. */ }
}
