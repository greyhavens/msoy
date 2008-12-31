//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.depot.ByteEnum;

public enum TransactionType
    implements IsSerializable, ByteEnum
{
    // NOTE: transaction types may be added, removed, or reordered, but the "byteValue" must
    // never change!
    OTHER(0),
    ITEM_PURCHASE(1),
    CREATOR_PAYOUT(2),
    AFFILIATE_PAYOUT(3),
    AWARD(4),
    BLING_POOL(5),
    BARS_BOUGHT(6),
    SPENT_FOR_EXCHANGE(7),
    RECEIVED_FROM_EXCHANGE(8),
    CASHED_OUT(9),
    REQUEST_CASH_OUT(10),
    CANCEL_CASH_OUT(11),
    SUPPORT_ADJUST(12),
    CHARITY_PAYOUT(13),
    CHANGE_IN_COINS(14),
    REFUND_GIVEN(15),
    REFUND_DEDUCTED(16),
    ;

    // Required by ByteEnum
    public static TransactionType fromByte (byte value)
    {
        for (TransactionType type : values()) {
            if (type.toByte() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid byte for TransactionType creation: " + value);
    }

    // from ByteEnum
    public byte toByte ()
    {
        return _byteValue;
    }

    /** Constructor. */
    private TransactionType (int byteValue)
    {
        _byteValue = (byte)byteValue;
    }

    /** The byte value. */
    protected transient byte _byteValue;
}
