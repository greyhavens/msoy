//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.TransactionType;

/**
 * Domain object representing an entry in a member's account history. The account history keeps
 * track of all changes to money in a member's account over time.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices = {
    @Index(name = "ixMemberId", fields = { MoneyTransactionRecord.MEMBER_ID }),
    @Index(name = "ixTimestamp", fields = { MoneyTransactionRecord.TIMESTAMP }),
    @Index(name = "ixTransactionType", fields = { MoneyTransactionRecord.TRANSACTION_TYPE }),
    @Index(name = "ixCurrency", fields = { MoneyTransactionRecord.CURRENCY }) })
public class MoneyTransactionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #id} field. */
    public static final String ID = "id";

    /** The qualified column identifier for the {@link #id} field. */
    public static final ColumnExp ID_C =
        new ColumnExp(MoneyTransactionRecord.class, ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MoneyTransactionRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #timestamp} field. */
    public static final String TIMESTAMP = "timestamp";

    /** The qualified column identifier for the {@link #timestamp} field. */
    public static final ColumnExp TIMESTAMP_C =
        new ColumnExp(MoneyTransactionRecord.class, TIMESTAMP);

    /** The column identifier for the {@link #transactionType} field. */
    public static final String TRANSACTION_TYPE = "transactionType";

    /** The qualified column identifier for the {@link #transactionType} field. */
    public static final ColumnExp TRANSACTION_TYPE_C =
        new ColumnExp(MoneyTransactionRecord.class, TRANSACTION_TYPE);

    /** The column identifier for the {@link #currency} field. */
    public static final String CURRENCY = "currency";

    /** The qualified column identifier for the {@link #currency} field. */
    public static final ColumnExp CURRENCY_C =
        new ColumnExp(MoneyTransactionRecord.class, CURRENCY);

    /** The column identifier for the {@link #amount} field. */
    public static final String AMOUNT = "amount";

    /** The qualified column identifier for the {@link #amount} field. */
    public static final ColumnExp AMOUNT_C =
        new ColumnExp(MoneyTransactionRecord.class, AMOUNT);

    /** The column identifier for the {@link #balance} field. */
    public static final String BALANCE = "balance";

    /** The qualified column identifier for the {@link #balance} field. */
    public static final ColumnExp BALANCE_C =
        new ColumnExp(MoneyTransactionRecord.class, BALANCE);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(MoneyTransactionRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #subjectType} field. */
    public static final String SUBJECT_TYPE = "subjectType";

    /** The qualified column identifier for the {@link #subjectType} field. */
    public static final ColumnExp SUBJECT_TYPE_C =
        new ColumnExp(MoneyTransactionRecord.class, SUBJECT_TYPE);

    /** The column identifier for the {@link #subjectIdType} field. */
    public static final String SUBJECT_ID_TYPE = "subjectIdType";

    /** The qualified column identifier for the {@link #subjectIdType} field. */
    public static final ColumnExp SUBJECT_ID_TYPE_C =
        new ColumnExp(MoneyTransactionRecord.class, SUBJECT_ID_TYPE);

    /** The column identifier for the {@link #subjectId} field. */
    public static final String SUBJECT_ID = "subjectId";

    /** The qualified column identifier for the {@link #subjectId} field. */
    public static final ColumnExp SUBJECT_ID_C =
        new ColumnExp(MoneyTransactionRecord.class, SUBJECT_ID);

    /** The column identifier for the {@link #referenceTxId} field. */
    public static final String REFERENCE_TX_ID = "referenceTxId";

    /** The qualified column identifier for the {@link #referenceTxId} field. */
    public static final ColumnExp REFERENCE_TX_ID_C =
        new ColumnExp(MoneyTransactionRecord.class, REFERENCE_TX_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    public static Function<MoneyTransactionRecord, MoneyTransaction> TO_TRANSACTION =
        new Function<MoneyTransactionRecord, MoneyTransaction>() {
        public MoneyTransaction apply (MoneyTransactionRecord record) {
            return record.toMoneyTransaction();
        }
    };
    
    /** ID of this record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** ID of the member this record is for. */
    public int memberId;

    /** Time this transaction was performed. */
    public Timestamp timestamp;

    /** Type of transaction this history record was for. */
    @Column(defaultValue = "0")
    public TransactionType transactionType;

    /** Type of money modified. */
    public Currency currency;

    /** Amount debited/credited. */
    public int amount;

    /** The member's balance of this currency after this transaction. */
    public int balance;

    /** A translatable description of the transaction. */
    public String description;

    /** A code indicating the type of the subject of this transaction. */
    public byte subjectType;

    /** An optional divider of the subjectId. */
    public byte subjectIdType;

    /** The id of the subject of this transaction. */
    public int subjectId;
    
    /** For certain types of transactions, the reference transaction this was in response to. */
    public int referenceTxId;

    /**
     * Create a new MoneyTransactionRecord.
     */
    public MoneyTransactionRecord (
        int memberId, Currency currency, int amount, int balance)
    {
        this.memberId = memberId;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.currency = currency;
        this.amount = amount;
        this.balance = balance;
    }

    /**
     * A convenience method to fill in the specified fields.
     */
    public void fill (TransactionType transType, String description, Object subject)
    {
        this.transactionType = transType;
        this.description = description;

        // MUCH TODO ABOUT NOTHING
        if (subject != null) {
            if (subject instanceof CatalogIdent) {
                this.subjectType = (byte)1; // TODO! TODO! TODO!
                CatalogIdent ident = (CatalogIdent) subject;
                this.subjectIdType = ident.type;
                this.subjectId = ident.catalogId;

            } else if (subject instanceof ItemIdent) {
                this.subjectType = (byte)2; // TODO! TODO! TODO!
                ItemIdent ident = (ItemIdent) subject;
                this.subjectIdType = ident.type;
                this.subjectId = ident.itemId;

            } else {
                throw new RuntimeException("Unknown subject: " + subject);
            }
        }
    }

    /** Suitable for unserialization. */
    public MoneyTransactionRecord ()
    {
    }
    
    public MoneyTransaction toMoneyTransaction ()
    {
        return new MoneyTransaction(
            memberId, timestamp, transactionType, currency, amount, balance, description);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MoneyTransactionRecord}
     * with the supplied key values.
     */
    public static Key<MoneyTransactionRecord> getKey (int id)
    {
        return new Key<MoneyTransactionRecord>(
                MoneyTransactionRecord.class,
                new String[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
