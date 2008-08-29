//
// $Id$

package com.threerings.msoy.money.server.persist;

import com.samskivert.jdbc.depot.ByteEnum;
import com.threerings.msoy.money.data.all.TransactionType;

public enum PersistentTransactionType implements ByteEnum {
    OTHER(0),
    ITEM_PURCHASE(1), 
    CREATOR_PAYOUT(2),
    AFFILIATE_PAYOUT(3),
    AWARD(4), 
    GAME_PLAYS(5),
    BARS_BOUGHT(6);
    
    public static PersistentTransactionType fromTransactionType (final TransactionType type) 
    {
        if (type == null) {
            return null;
        }
        
        switch (type) {
        case ITEM_PURCHASE: return PersistentTransactionType.ITEM_PURCHASE;
        case CREATOR_PAYOUT: return PersistentTransactionType.CREATOR_PAYOUT;
        case AFFILIATE_PAYOUT: return PersistentTransactionType.AFFILIATE_PAYOUT;
        case AWARD: return PersistentTransactionType.AWARD;
        case GAME_PLAYS: return PersistentTransactionType.GAME_PLAYS;
        case BARS_BOUGHT: return PersistentTransactionType.BARS_BOUGHT;
        case OTHER: return PersistentTransactionType.OTHER;
        }
        throw new IllegalArgumentException("Invalid transaction type: " + type);
    }
    
    public static PersistentTransactionType fromByte (final byte value)
    {
        for (final PersistentTransactionType type : values()) {
            if (type._value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException(
            "Invalid byte value for PersistentTransactionType: " + value);
    }

    public byte toByte ()
    {
        return _value;
    }
    
    public TransactionType toTransactionType ()
    {
        switch (this) {
        case ITEM_PURCHASE: return TransactionType.ITEM_PURCHASE;
        case CREATOR_PAYOUT: return TransactionType.CREATOR_PAYOUT;
        case AFFILIATE_PAYOUT: return TransactionType.AFFILIATE_PAYOUT;
        case AWARD: return TransactionType.AWARD;
        case GAME_PLAYS: return TransactionType.GAME_PLAYS;
        case BARS_BOUGHT: return TransactionType.BARS_BOUGHT;
        case OTHER: return TransactionType.OTHER;
        }
        throw new IllegalArgumentException("Cannot convert this to transaction type: " + this);
    }

    private PersistentTransactionType (final int value)
    {
        _value = (byte)value;
    }

    private final byte _value;
}
