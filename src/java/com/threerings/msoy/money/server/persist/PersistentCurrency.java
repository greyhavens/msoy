//
// $Id$

package com.threerings.msoy.money.server.persist;

import com.samskivert.jdbc.depot.ByteEnum;
import com.threerings.msoy.money.data.all.Currency;

public enum PersistentCurrency implements ByteEnum
{
    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    BARS(0),

    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    COINS(1),

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    BLING(2);

    public static PersistentCurrency fromCurrency (final Currency currency) 
    {
        if (currency == null) {
            return null;
        }
        
        switch (currency) {
        case BARS: return PersistentCurrency.BARS;
        case COINS: return PersistentCurrency.COINS;
        case BLING: return PersistentCurrency.BLING;
        default: throw new IllegalArgumentException("Invalid currency: " + currency);
        }
    }
    
    public static PersistentCurrency fromByte (final byte value)
    {
        // Note: this is not ideal. We should iterate over values() and check each value's
        // toByte() to see if it's the same as the passed-in value. That reduces the number
        // of places that things must be changed if we add a new currency and is generally
        // the right way to do things with enums.
        switch (value) {
        case 0: return BARS;
        case 1: return COINS;
        case 2: return BLING;
        default:
            throw new IllegalArgumentException(
                "Invalid byte value for PersistentCurrency: " + value);
        }
    }

    public byte toByte ()
    {
        return _value;
    }
    
    public Currency toCurrency ()
    {
        switch (this) {
        case BARS: return Currency.BARS;
        case COINS: return Currency.COINS;
        case BLING: return Currency.BLING;
        }
        throw new IllegalArgumentException("Cannot convert this to Currency: " + this);
    }

    private PersistentCurrency (final int value)
    {
        _value = (byte)value;
    }

    private final byte _value;
}

