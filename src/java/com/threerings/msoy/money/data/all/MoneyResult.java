//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MoneyResult
    implements IsSerializable
{
    /** The user's current coin balance, or null. */
    public Integer coinBalance;

    /** The user's current bar balance, or null. */
    public Integer barBalance;

    /** The user's complete bling balance and info, or null. */
    public BlingInfo blingInfo;

    public MoneyResult (Integer coinBalance, Integer barBalance, BlingInfo blingInfo)
    {
        this.coinBalance = coinBalance;
        this.barBalance = barBalance;
        this.blingInfo = blingInfo;
    }

    public MoneyResult () 
    {
        // For Serialization
    }
}
