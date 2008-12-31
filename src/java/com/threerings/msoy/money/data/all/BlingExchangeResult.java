//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BlingExchangeResult
    implements IsSerializable
{
    /** The user's current bar balance. */
    public int barBalance;

    /** The user's complete bling balance and info, or null. */
    public BlingInfo blingInfo;

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
