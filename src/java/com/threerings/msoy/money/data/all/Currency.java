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
    COINS(0) {
        public boolean isValidCost (int cost) {
            return (cost >= 0);
        }
    },

    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    BARS(1) {
        public boolean isValidCost (int cost) {
            return (cost > 0);
        }
    },

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    BLING(2) {
        public boolean isValidCost (int cost) {
            return false; // Can't ever list in bling
        }
    };

    /** Returns whether or not some cost is a valid amount to be listed with this currency. */
    public abstract boolean isValidCost (int cost);

    /**
     * Translate a byte back into the Currency instance- required by ByteEnum.
     */
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
     * Get the small icon for this currency, WITH a leading /.
     */
    public String getSmallIcon ()
    {
        return "/images/ui/" + toString().toLowerCase() + "_small.png";
    }

    /**
     * Get the large icon for this currency, WITH a leading /.
     */
    public String getLargeIcon ()
    {
        return "/images/ui/" + toString().toLowerCase() + "_large.png";
    }

    /**
     * Format a currency value.
     */
    public String format (int value)
    {
        // Note: I'm doing this by hand instead of using NumberFormats because of GWT
        // and because we pretty much have to do this same logic in actionscript, anyway
        String postfix = "";
        if (this == BLING) {
            int cents = Math.abs(value % 100);
            value /= 100;
            postfix = "." + (cents / 10) + (cents % 10); // always print two decimal places
        }

        // put commas in, superhackystyle
        String s = String.valueOf(value);
        int length = s.length();
        while (length > 3) {
            length -= 3;
            postfix = "," + s.substring(length) + postfix;
            s = s.substring(0, length);
        }

        return s + postfix;
    }

    public int parse (String text)
    {
        float value = Float.valueOf(text.replace(",", ""));
        if (this == BLING) {
            value *= 100;
        }
        return (int)value;
    }

    /**
     * Used to display just the name of the currency. "Coins", "Bars"
     */
    public String getLabel ()
    {
        return "l." + toString().toLowerCase();
    }

    /**
     * Used when translating a currency name as part of a larger message: "coins", "bars".
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
