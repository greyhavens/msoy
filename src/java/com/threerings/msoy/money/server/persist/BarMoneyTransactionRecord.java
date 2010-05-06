//
// $Id: MoneyTransactionRecord.java 18060 2009-09-11 20:53:36Z jamie $

package com.threerings.msoy.money.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.money.data.all.Currency;

/**
 */
public class BarMoneyTransactionRecord extends MoneyTransactionRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<BarMoneyTransactionRecord> _R = BarMoneyTransactionRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp TRANSACTION_TYPE = colexp(_R, "transactionType");
    public static final ColumnExp AMOUNT = colexp(_R, "amount");
    public static final ColumnExp BALANCE = colexp(_R, "balance");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp SUBJECT_TYPE = colexp(_R, "subjectType");
    public static final ColumnExp SUBJECT_ID_TYPE = colexp(_R, "subjectIdType");
    public static final ColumnExp SUBJECT_ID = colexp(_R, "subjectId");
    public static final ColumnExp REFERENCE_TX_ID = colexp(_R, "referenceTxId");
    public static final ColumnExp REFERENCE_MEMBER_ID = colexp(_R, "referenceMemberId");
    // AUTO-GENERATED: FIELDS END

    public BarMoneyTransactionRecord ()
    {
        super();
    }

    public BarMoneyTransactionRecord (int memberId, int amount, int balance,
            boolean accAffected, long accBalance)
    {
        super(memberId, amount, balance, accAffected, accBalance);
    }

    @Override // from MoneyTransactionRecord
    public Currency getCurrency ()
    {
        return Currency.BARS;
    }

    /**
     * Defines the subject multikey index.
     */
    public static ColumnExp[] ixSubject ()
    {
        return new ColumnExp[] { SUBJECT_TYPE, SUBJECT_ID_TYPE, SUBJECT_ID };
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link BarMoneyTransactionRecord}
     * with the supplied key values.
     */
    public static Key<BarMoneyTransactionRecord> getKey (int id)
    {
        return new Key<BarMoneyTransactionRecord>(
                BarMoneyTransactionRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
