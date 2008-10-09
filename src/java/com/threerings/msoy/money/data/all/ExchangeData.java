//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains info on a bar/coin exchange.
 */
public class ExchangeData
    implements IsSerializable
{
    public Date timestamp;
    public int bars;
    public int coins;
    public float rate;
    public int referenceTxId;

    /** Suitable for unserialization. */
    public ExchangeData () {}

    /**
     * ExchangeData
     */
    public ExchangeData (Date timestamp, int bars, int coins, float rate, int referenceTxId)
    {
        this.timestamp = timestamp;
        this.bars = bars;
        this.coins = coins;
        this.rate = rate;
        this.referenceTxId = referenceTxId;
    }
}
