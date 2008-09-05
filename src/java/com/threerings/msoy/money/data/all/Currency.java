//
// $Id$

package com.threerings.msoy.money.data.all;

import com.samskivert.jdbc.depot.ByteEnum;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Indicates the type of money.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
public enum Currency
    implements ByteEnum, IsSerializable
{
    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    COINS(0),

    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    BARS(1),

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    BLING(2);

    // Required by ByteEnum
    public static Currency fromByte (byte value)
    {
        for (Currency cur : values()) {
            if (cur.toByte() == value) {
                return cur;
            }
        }
        throw new IllegalArgumentException("Invalid byte for Currency creation: " + value);
    }

    // from ByteEnum
    public byte toByte ()
    {
        return _byteValue;
    }

    /**
     * Used to display just the name of the currency.
     */
    public String getLabel ()
    {
        return "l." + toString().toLowerCase();
    }

    /**
     * Used when translating a currency with a value:
     * MessageBundle.get(currency.getKey(), amount) == "5 bars", or "1 bar"
     */
    public String getKey ()
    {
        return "m." + toString().toLowerCase();
    }

    /** Constructor. */
    private Currency (int byteValue)
    {
        _byteValue = (byte)byteValue;
    }

    /** The byte value. No need to serialize this to flash/GWT. */
    protected transient byte _byteValue;
}
