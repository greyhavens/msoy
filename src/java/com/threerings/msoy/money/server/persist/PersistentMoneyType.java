//
// $Id$

package com.threerings.msoy.money.server.persist;

import com.samskivert.jdbc.depot.ByteEnum;
import com.threerings.msoy.money.data.all.MoneyType;

public enum PersistentMoneyType implements ByteEnum
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

    public static PersistentMoneyType fromMoneyType (final MoneyType type) 
    {
        if (type == null) {
            return null;
        }
        
        switch (type) {
        case BARS: return PersistentMoneyType.BARS;
        case COINS: return PersistentMoneyType.COINS;
        case BLING: return PersistentMoneyType.BLING;
        }
        throw new IllegalArgumentException("Invalid money type: " + type);
    }
    
    public static PersistentMoneyType fromByte (final byte value)
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
                "Invalid byte value for PersistentMoneyType: " + value);
        }
    }

    public byte toByte ()
    {
        return _value;
    }
    
    public MoneyType toMoneyType ()
    {
        switch (this) {
        case BARS: return MoneyType.BARS;
        case COINS: return MoneyType.COINS;
        case BLING: return MoneyType.BLING;
        }
        throw new IllegalArgumentException("Cannot convert this to money type: " + this);
    }

    private PersistentMoneyType (final int value)
    {
        _value = (byte)value;
    }

    private final byte _value;
}

