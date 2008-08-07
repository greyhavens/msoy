//
// $Id$

package com.threerings.msoy.money.server;

import net.jcip.annotations.Immutable;

import com.samskivert.jdbc.depot.ByteEnum;

/**
 * Indicates the type of money.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Immutable
public enum MoneyType implements ByteEnum {
    /** Bars are usually purchased for some real money amount and may be required to purchase some items. */
    BARS(0),

    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    COINS(1),

    /**
     * Bling is awarded when other players purchase or use some content created by a content creator.
     * It can be exchanged for real money.
     */
    BLING(2);
    
    public static MoneyType fromByte(final byte value) 
    {
        switch(value) {
        case 0:
            return BARS;
        case 1:
            return COINS;
        case 2:
            return BLING;
        default:
            throw new IllegalArgumentException("Invalid byte value for MoneyType: " + value);
        }
    }
    
    public byte toByte ()
    {
        return _value;
    }

    private MoneyType (final int value)
    {
        this._value = (byte)value;
    }
    
    private final byte _value;
}
