//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BlingInfo
    implements IsSerializable
{
    public /* final */ int bling;
    public /* final */ int blingWorth;
    
    public BlingInfo (int bling, int blingWorth)
    {
        this.bling = bling;
        this.blingWorth = blingWorth;
    }
    
    /** For serialization purposes. */
    public BlingInfo () { }
}
