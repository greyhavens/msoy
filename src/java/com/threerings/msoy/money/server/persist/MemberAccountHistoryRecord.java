//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;

/**
 * Domain object representing an entry in a member's account history. The account history keeps
 * track of all changes to money in a member's account over time.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices = {
    @Index(name = "ixMemberId", fields = { MemberAccountHistoryRecord.MEMBER_ID }),
    @Index(name = "ixTimestamp", fields = { MemberAccountHistoryRecord.TIMESTAMP }),
    @Index(name = "ixType", fields = { MemberAccountHistoryRecord.TYPE }) })
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
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberAccountHistoryRecord}
     * with the supplied key values.
     */
    public static Key<MemberAccountHistoryRecord> getKey (int id)
    {
        return new Key<MemberAccountHistoryRecord>(
                MemberAccountHistoryRecord.class,
                new String[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END

    public static final int SCHEMA_VERSION = 2;

    /**
     * Creates an account history record involving some particular item.
     * 
     * @param memberId ID of the member
     * @param timestamp Time the change was made.
     * @param type Type of money (coins/bars/bling) modified.
     * @param amount Amount modified.
     * @param spent True if the amount was debited from the account, false if the amount was
     * credited.
     * @param description Description of the transaction.
     * @param itemId ID of the item
     * @param itemType Type of the item.
     */
    public MemberAccountHistoryRecord (final int memberId, final Date timestamp,
            final MoneyType type, final double amount, final boolean spent,
            final String description, final ItemIdent item)
    {
        this.memberId = memberId;
        this.timestamp = new Timestamp(timestamp.getTime());
        this.type = type;
        this.amount = amount;
        this.spent = spent;
        this.description = description;
        if (item != null) {
            this.itemId = item.itemId;
            this.itemType = item.type;
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
    public MemberAccountHistoryRecord (final int memberId, final Date timestamp,
            final MoneyType type, final double amount, final boolean spent,
            final String description)
    {
        this.memberId = memberId;
        this.timestamp = new Timestamp(timestamp.getTime());
        this.type = type;
        this.amount = amount;
        this.spent = spent;
        this.description = description;
    }

    /** Not part of the API. For depot's eyes only. */
    public MemberAccountHistoryRecord ()
    {}

    /**
     * ID of the member account this history is for.
     */
    public int getMemberId ()
    {
        return memberId;
    }

    /**
     * Time the transaction was performed.
     */
    public Date getTimestamp ()
    {
        return timestamp;
    }

    /**
     * The amount that was exchanged in the transaction.
     */
    public double getAmount ()
    {
        return amount;
    }

    /**
     * The amount that was exchanged in the transaction. This will be negative if spent, positive
     * otherwise.
     */
    public double getSignedAmount ()
    {
        return spent ? -amount : amount;
    }

    /**
     * If true, this amount was deducted from the account. Otherwise it was credited to the
     * account.
     */
    public boolean isSpent ()
    {
        return spent;
    }

    /**
     * Description of the transaction.
     */
    public String getDescription ()
    {
        return description;
    }

    /**
     * Type of money that was transferred.
     */
    public MoneyType getType ()
    {
        return type;
    }

    /**
     * ID of the item that was involved in this transaction.
     * @return
     */
    public int getItemId ()
    {
        return itemId;
    }

    /**
     * Type of the item that was involved in this transaction.
     */
    public int getItemType ()
    {
        return itemType;
    }

    public int getId ()
    {
        return id;
    }

    public MoneyHistory createMoneyHistory ()
    {
        return new MoneyHistory(memberId, timestamp, type, amount, spent, description,
            itemId == 0 ? null : new ItemIdent((byte)itemType, itemId));
    }

    // These are not part of the api! They should be private (depot requirement...)

    /** ID of this record. Note: this is not part of the API, do not use it. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** ID of the member this record is for. Note: this is not part of the API, do not use it. */
    public int memberId;

    /** Time this transaction was performed. Note: this is not part of the API, do not use it. */
    public Timestamp timestamp;

    /** Type of money modified. Note: this is not part of the API, do not use it. */
    public MoneyType type;

    /** Amount debited/credited. Note: this is not part of the API, do not use it. */
    public double amount;

    /**
     * True if the amount was debited, otherwise it was credited. Note: this is not part of the
     * API, do not use it.
     */
    public boolean spent;

    /** Description of the transaction. Note: this is not part of the API, do not use it. */
    public String description;

    /**
     * ID of the item involved in this transaction, or 0 otherwise. Note: this is not part of the
     * API, do not use it.
     */
    public int itemId;

    /**
     * Type of the item involved in this transaction, if provided. Note: this is not part of the
     * API, do not use it.
     */
    public int itemType;
}
