//
// $Id: MoneyTransactionRecord.java 18060 2009-09-11 20:53:36Z jamie $

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;

/**
 */
public class CoinMoneyTransactionRecord extends MoneyTransactionRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CoinMoneyTransactionRecord> _R = CoinMoneyTransactionRecord.class;
    public static final ColumnExp<Integer> ID = colexp(_R, "id");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Timestamp> TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp<TransactionType> TRANSACTION_TYPE = colexp(_R, "transactionType");
    public static final ColumnExp<Integer> AMOUNT = colexp(_R, "amount");
    public static final ColumnExp<Integer> BALANCE = colexp(_R, "balance");
    public static final ColumnExp<String> DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp<Byte> SUBJECT_TYPE = colexp(_R, "subjectType");
    public static final ColumnExp<MsoyItemType> SUBJECT_ID_TYPE = colexp(_R, "subjectIdType");
    public static final ColumnExp<Integer> SUBJECT_ID = colexp(_R, "subjectId");
    public static final ColumnExp<Integer> REFERENCE_TX_ID = colexp(_R, "referenceTxId");
    public static final ColumnExp<Integer> REFERENCE_MEMBER_ID = colexp(_R, "referenceMemberId");
    // AUTO-GENERATED: FIELDS END

    public CoinMoneyTransactionRecord ()
    {
        super();
    }

    public CoinMoneyTransactionRecord (int memberId, int amount, int balance,
            boolean accAffected, long accBalance)
    {
        super(memberId, amount, balance, accAffected, accBalance);
    }

    @Override // from MoneyTransactionRecord
    public Currency getCurrency ()
    {
        return Currency.COINS;
    }

    /**
     * Defines the subject multikey index.
     */
    public static ColumnExp<?>[] ixSubject ()
    {
        return new ColumnExp<?>[] { SUBJECT_TYPE, SUBJECT_ID_TYPE, SUBJECT_ID };
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CoinMoneyTransactionRecord}
     * with the supplied key values.
     */
    public static Key<CoinMoneyTransactionRecord> getKey (int id)
    {
        return newKey(_R, id);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ID); }
    // AUTO-GENERATED: METHODS END
}
