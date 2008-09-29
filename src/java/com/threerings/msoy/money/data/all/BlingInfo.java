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
