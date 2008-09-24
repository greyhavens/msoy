//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BlingExchangeResult
    implements IsSerializable
{
    public /* final */ int barBalance;
    public /* final */ BlingInfo blingInfo;
    
    public BlingExchangeResult (int barBalance, BlingInfo blingInfo)
    {
        this.barBalance = barBalance;
        this.blingInfo = blingInfo;
    }
    
    public BlingExchangeResult () 
    {
        // For Serialization
    }
}
