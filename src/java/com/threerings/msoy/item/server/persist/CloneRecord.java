//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations

import com.threerings.msoy.money.data.all.Currency;

@Entity(indices={
    @Index(name="ixOwner", fields={ CloneRecord.OWNER_ID }),
    @Index(name="ixOriginalItem", fields={ CloneRecord.ORIGINAL_ITEM_ID }),
    @Index(name="ixLocation", fields={ CloneRecord.LOCATION })
})
public abstract class CloneRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The column identifier for the {@link #originalItemId} field. */
    public static final String ORIGINAL_ITEM_ID = "originalItemId";

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The column identifier for the {@link #purchaseTime} field. */
    public static final String PURCHASE_TIME = "purchaseTime";

    /** The column identifier for the {@link #currency} field. */
    public static final String CURRENCY = "currency";

    /** The column identifier for the {@link #amountPaid} field. */
    public static final String AMOUNT_PAID = "amountPaid";

    /** The column identifier for the {@link #used} field. */
    public static final String USED = "used";

    /** The column identifier for the {@link #location} field. */
    public static final String LOCATION = "location";

    /** The column identifier for the {@link #lastTouched} field. */
    public static final String LAST_TOUCHED = "lastTouched";

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The column identifier for the {@link #mediaHash} field. */
    public static final String MEDIA_HASH = "mediaHash";

    /** The column identifier for the {@link #mediaStamp} field. */
    public static final String MEDIA_STAMP = "mediaStamp";
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
    public int originalItemId;

    /** The owner of this clone. */
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
