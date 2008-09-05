//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import java.util.Date;

import net.jcip.annotations.NotThreadSafe;

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

import com.threerings.msoy.money.data.all.MoneyHistory;

/**
 * Domain object representing an entry in a member's account history. The account history keeps
 * track of all changes to money in a member's account over time.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices = {
    @Index(name = "ixMemberId", fields = { MemberAccountHistoryRecord.MEMBER_ID }),
    @Index(name = "ixTimestamp", fields = { MemberAccountHistoryRecord.TIMESTAMP }),
    @Index(name = "ixType", fields = { MemberAccountHistoryRecord.TYPE }), 
    @Index(name = "ixTransactionType", fields = { MemberAccountHistoryRecord.TRANSACTION_TYPE }) })
@NotThreadSafe
public class MemberAccountHistoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #id} field. */
    public static final String ID = "id";

    /** The qualified column identifier for the {@link #id} field. */
    public static final ColumnExp ID_C =
        new ColumnExp(MemberAccountHistoryRecord.class, ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberAccountHistoryRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #timestamp} field. */
    public static final String TIMESTAMP = "timestamp";

    /** The qualified column identifier for the {@link #timestamp} field. */
    public static final ColumnExp TIMESTAMP_C =
        new ColumnExp(MemberAccountHistoryRecord.class, TIMESTAMP);

    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(MemberAccountHistoryRecord.class, TYPE);

    /** The column identifier for the {@link #amount} field. */
    public static final String AMOUNT = "amount";

    /** The qualified column identifier for the {@link #amount} field. */
    public static final ColumnExp AMOUNT_C =
        new ColumnExp(MemberAccountHistoryRecord.class, AMOUNT);

    /** The column identifier for the {@link #spent} field. */
    public static final String SPENT = "spent";

    /** The qualified column identifier for the {@link #spent} field. */
    public static final ColumnExp SPENT_C =
        new ColumnExp(MemberAccountHistoryRecord.class, SPENT);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(MemberAccountHistoryRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(MemberAccountHistoryRecord.class, ITEM_ID);

    /** The column identifier for the {@link #itemType} field. */
    public static final String ITEM_TYPE = "itemType";

    /** The qualified column identifier for the {@link #itemType} field. */
    public static final ColumnExp ITEM_TYPE_C =
        new ColumnExp(MemberAccountHistoryRecord.class, ITEM_TYPE);

    /** The column identifier for the {@link #transactionType} field. */
    public static final String TRANSACTION_TYPE = "transactionType";

    /** The qualified column identifier for the {@link #transactionType} field. */
    public static final ColumnExp TRANSACTION_TYPE_C =
        new ColumnExp(MemberAccountHistoryRecord.class, TRANSACTION_TYPE);

    /** The column identifier for the {@link #referenceTxId} field. */
    public static final String REFERENCE_TX_ID = "referenceTxId";

    /** The qualified column identifier for the {@link #referenceTxId} field. */
    public static final ColumnExp REFERENCE_TX_ID_C =
        new ColumnExp(MemberAccountHistoryRecord.class, REFERENCE_TX_ID);
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberAccountHistoryRecord}
     * with the supplied key values.
     */
    public static Key<MemberAccountHistoryRecord> getKey (final int id)
    {
        return new Key<MemberAccountHistoryRecord>(
                MemberAccountHistoryRecord.class,
                new String[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END

    public static final int SCHEMA_VERSION = 3;
    
    /** ID of this record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** ID of the member this record is for. */
    public int memberId;

    /** Time this transaction was performed. */
    public Timestamp timestamp;

    /** Type of money modified. */
    public PersistentCurrency type;

    /** Amount debited/credited. */
    public double amount;

    /** True if the amount was debited, otherwise it was credited. */
    public boolean spent;

    /** Description of the transaction. */
    public String description;

    /** ID of the item involved in this transaction, or 0 otherwise. */
    // TODO: rename this to catalogId ? ?? 
    public int itemId;

    /** Type of the item involved in this transaction, if provided. */
    public int itemType;

    /** Type of transaction this history record was for. */
    @Column(defaultValue = "0")
    public PersistentTransactionType transactionType;
    
    /** For certain types of transactions, the reference transaction this was in response to. */
    public int referenceTxId;

    /**
     * Creates an account history record involving some particular item.
     * 
     * @param memberId ID of the member
     * @param timestamp Time the change was made.
     * @param currency Currency (coins/bars/bling) modified.
     * @param amount Amount modified.
     * @param spent True if the amount was debited from the account, false if the amount was
     * credited.
     * @param description Description of the transaction.
     * @param itemId ID of the item
     * @param itemType Type of the item.
     */
    public MemberAccountHistoryRecord (
        int memberId, Timestamp timestamp, PersistentCurrency currency, double amount,
        PersistentTransactionType transactionType, boolean spent, 
        String description, CatalogIdent item)
    {
        this.memberId = memberId;
        this.timestamp = timestamp;
        this.type = currency;
        this.amount = amount;
        this.spent = spent;
        this.description = description;
        this.transactionType = transactionType;
        if (item != null) {
            itemType = item.type;
            // TODO: rename itemId to catalogId
            itemId = item.catalogId;
        }
    }

    /**
     * Creates an account history record representing a generic transaction made on an account.
     * 
     * @param memberId ID of the member
     * @param timestamp Time the change was made.
     * @param type Type of money (coins/bars/bling) modified.
     * @param amount Amount modified.
     * @param spent True if the amount was debited from the account, false if the amount was
     * credited.
     * @param description Description of the transaction.
     */
    public MemberAccountHistoryRecord (
        int memberId, Timestamp timestamp, PersistentCurrency currency, double amount,
        PersistentTransactionType transactionType, boolean spent, String description)
    {
        this(memberId, timestamp, currency, amount, transactionType, spent, description, null);
    }

    /** Not part of the API. For depot's eyes only. */
    public MemberAccountHistoryRecord ()
    {}
    
    /**
     * The amount that was exchanged in the transaction. This will be negative if spent, positive
     * otherwise.
     */
    public double getSignedAmount ()
    {
        return spent ? -amount : amount;
    }

    public MoneyHistory createMoneyHistory (final MoneyHistory referenceTx)
    {
        return new MoneyHistory(memberId, timestamp, type.toCurrency(), amount, 
            transactionType.toTransactionType(), spent, description, 
            itemId == 0 ? null : new ItemIdent((byte)itemType, itemId), referenceTx);
    }
}
