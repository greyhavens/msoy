//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BlingInfo
    implements IsSerializable
{
    /** An amount of centibling. */
    public int bling;

    /** The worth in US pennies. */
    public int blingWorth;
    
    /** The amount of bling the user has requested to cash out, or 0 if no request. */
    public int cashedOutBling;
    
    /** The worth of the bling the user has requested to cash out. */
    public int cashedOutBlingWorth;
    
    public BlingInfo (int bling, int blingWorth, int cashedOutBling, 
        int cashedOutBlingWorth)
    {
        this.bling = bling;
        this.blingWorth = blingWorth;
        this.cashedOutBling = cashedOutBling;
        this.cashedOutBlingWorth = cashedOutBlingWorth;
    }
    
    /** For serialization purposes. */
    public BlingInfo () { }
}
