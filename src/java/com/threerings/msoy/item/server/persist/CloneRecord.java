//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.*; // for Depot annotations

import com.threerings.msoy.money.data.all.Currency;

@Entity
public abstract class CloneRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CloneRecord> _R = CloneRecord.class;
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp ORIGINAL_ITEM_ID = colexp(_R, "originalItemId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp PURCHASE_TIME = colexp(_R, "purchaseTime");
    public static final ColumnExp CURRENCY = colexp(_R, "currency");
    public static final ColumnExp AMOUNT_PAID = colexp(_R, "amountPaid");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp MEDIA_HASH = colexp(_R, "mediaHash");
    public static final ColumnExp MEDIA_STAMP = colexp(_R, "mediaStamp");
    // AUTO-GENERATED: FIELDS END

    public static final int BASE_SCHEMA_VERSION = 8;
    public static final int BASE_MULTIPLIER = 1000;
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** This clone's id, unique relative to all items of the same type. */
    @Id
    @GeneratedValue(generator="cloneId", strategy=GenerationType.TABLE, allocationSize=-1,
                    initialValue=-1)
    public int itemId;

    /** The id of the immutable item from which this was cloned. */
    @Index(name="ixOriginalItem")
    public int originalItemId;

    /** The owner of this clone. */
    @Index(name="ixOwner")
    public int ownerId;

    /** The time at which this clone was purchased from the catalog. */
    public Timestamp purchaseTime;

    /** The currency used to purchase this item. */
    public Currency currency;

    /** The amount (of the above currency) paid for this item. */
    public int amountPaid;

    /** How this item is being used (see Item.USED_AS_FURNITURE). */
    public byte used;

    /** Where it's being used. */
    @Index(name="ixLocation")
    public int location;

    /** The timestamp at which this item was last used or modified. */
    public Timestamp lastTouched;

    /** An override name or null, provided if the user renames a clone for convenience. */
    @Column(nullable=true)
    public String name;

    /** An override of the "main" media hash for this item.
     * This can currently only be the result of remixing, so the mimeType will be the
     * same as the original media mimeType, and should only be APPLICATION/ZIP. */
    @Column(nullable=true)
    public byte[] mediaHash;

    /** The time at which the mediaHash was set, so that we know when there is a new
     * version available out of the shop. Null when mediaHash is null. */
    @Column(nullable=true)
    public Timestamp mediaStamp;

    /**
     * Initialize a new clone with the specified values.
     */
    public void initialize (ItemRecord parent, int newOwnerId, Currency currency, int amountPaid)
    {
        long now = System.currentTimeMillis();
        this.originalItemId = parent.itemId;
        this.ownerId = newOwnerId;
        this.currency = currency;
        this.amountPaid = amountPaid;
        this.purchaseTime = new Timestamp(now);
        this.lastTouched = new Timestamp(now);
    }
}
