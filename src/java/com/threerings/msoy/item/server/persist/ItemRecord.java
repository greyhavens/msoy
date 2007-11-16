//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.ArrayList;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.FullTextIndex;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Table;

import com.samskivert.util.StringUtil;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * The base class for all digital items in the MSOY system.
 */
@Table(fullTextIndexes={
    @FullTextIndex(name=ItemRecord.FTS_ND, fieldNames={
        ItemRecord.NAME, ItemRecord.DESCRIPTION })})
@Entity(indices={
    @Index(name="locationIndex", fields={ ItemRecord.LOCATION } ),
    @Index(name="ixFlagged", fields={ ItemRecord.FLAGGED } ),
    @Index(name="ixMature", fields={ ItemRecord.MATURE } ),
    @Index(name="ixOwner", fields={ ItemRecord.OWNER_ID }),
    @Index(name="ixCreator", fields={ ItemRecord.CREATOR_ID })
})
public abstract class ItemRecord extends PersistentRecord implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The column identifier for the {@link #sourceId} field. */
    public static final String SOURCE_ID = "sourceId";

    /** The column identifier for the {@link #flagged} field. */
    public static final String FLAGGED = "flagged";

    /** The column identifier for the {@link #creatorId} field. */
    public static final String CREATOR_ID = "creatorId";

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The column identifier for the {@link #catalogId} field. */
    public static final String CATALOG_ID = "catalogId";

    /** The column identifier for the {@link #rating} field. */
    public static final String RATING = "rating";

    /** The column identifier for the {@link #used} field. */
    public static final String USED = "used";

    /** The column identifier for the {@link #location} field. */
    public static final String LOCATION = "location";

    /** The column identifier for the {@link #lastTouched} field. */
    public static final String LAST_TOUCHED = "lastTouched";

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The column identifier for the {@link #mature} field. */
    public static final String MATURE = "mature";

    /** The column identifier for the {@link #thumbMediaHash} field. */
    public static final String THUMB_MEDIA_HASH = "thumbMediaHash";

    /** The column identifier for the {@link #thumbMimeType} field. */
    public static final String THUMB_MIME_TYPE = "thumbMimeType";

    /** The column identifier for the {@link #thumbConstraint} field. */
    public static final String THUMB_CONSTRAINT = "thumbConstraint";

    /** The column identifier for the {@link #furniMediaHash} field. */
    public static final String FURNI_MEDIA_HASH = "furniMediaHash";

    /** The column identifier for the {@link #furniMimeType} field. */
    public static final String FURNI_MIME_TYPE = "furniMimeType";

    /** The column identifier for the {@link #furniConstraint} field. */
    public static final String FURNI_CONSTRAINT = "furniConstraint";
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on Name, Description */
    public static final String FTS_ND = "ND";

    public static final int BASE_SCHEMA_VERSION = 16;
    public static final int BASE_MULTIPLIER = 1000;

    /** This item's unique identifier. <em>Note:</em> this identifier is not globally unique among
     * all digital items. Each type of item has its own identifier space. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE, allocationSize=1)
    public int itemId;

    /** The item ID from which this object was cloned, or 0 if this is not a clone. This field is
     * not persisted to the database, but set when we load a clone. */
    @Computed(required=false)
    public int sourceId = 0;

    /** A bit-mask of flags that we need to know about every digital item without doing further
     * database lookups or network requests. */
    public byte flagged;

    /** The member id of the member that created this item. */
    public int creatorId;

    /** The member id of the member that owns this item, or 0 if it's not in any inventory;
     * e.g. it's listed in the catalog or a gifted item in a mail message. */
    public int ownerId;

    /** The id of the catalog listing for which this item is either the listed prototype (in which
     * case ownerId == 0) or the original (in which case ownerId != 0). */
    public int catalogId;

    /** The current rating of this item, from 1 to 5. */
    public float rating;

    /** How this item is being used (see {@link Item#USED_AS_FURNITURE}). */
    public byte used;

    /** Where it's being used. */
    public int location;

    /** The timestamp at which this item was last touched. */
    public Timestamp lastTouched;

    /** A user supplied name for this item. */
    public String name;

    /** A user supplied description for this item. */
    @Column(length=Item.MAX_DESCRIPTION_LENGTH)
    public String description;

    /** Whether or not this item represents mature content. */
    public boolean mature;

    /** A hash code identifying the media used to display this item's thumbnail representation. */
    @Column(nullable=true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** The size constraint on the {@link #thumbMediaHash} media. */
    public byte thumbConstraint;

    /** A hash code identifying the media used to display this item's furniture representation. */
    @Column(nullable=true)
    public byte[] furniMediaHash;

    /** The MIME type of the {@link #furniMediaHash} media. */
    public byte furniMimeType;

    /** The size constraint on the {@link #furniMediaHash} media. */
    public byte furniConstraint;

    /**
     * This is used to map {@link ItemRecord} concrete classes to item type
     * values.
     */
    public abstract byte getType ();

    @Override
    public int hashCode ()
    {
        return itemId;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof ItemRecord) {
            ItemRecord that = (ItemRecord) other;
            return (itemId == that.itemId) && (getType() == that.getType());
        }
        return false;
    }

    /**
     * Tests whether a given flag is set on this item.
     */
    public boolean isSet (byte flag)
    {
        return (flagged & flag) != 0;
    }

    /**
     * Sets a given flag to on or off.
     */
    public void setFlag (byte flag, boolean value)
    {
        flagged = (byte) (value ? flagged | flag : flagged ^ ~flag);
    }

    /**
     * Clears out any fields that should be reset when listing this item in the catalog.
     *
     * @param oldListing the previous catalog prototype item if this item has already been listed,
     * or null if it is being listed for the first time.
     */
    public void prepareForListing (ItemRecord oldListing)
    {
        itemId = 0;
        ownerId = 0;
        used = 0;
        location = 0;

        if (oldListing != null) {
            // we're going to replace the old catalog listing item
            itemId = oldListing.itemId;
            // inherit the average rating from the old item
            rating = oldListing.rating;
            // if the old listing was mature or the new item is mature we want the new listing to
            // be mature as well; there's no going back without listing anew
            mature = oldListing.mature || mature;
        }
    }

    /**
     * Clears out any fields that should be reset when remixing an item.
     */
    public void prepareForRemixing ()
    {
        itemId = 0;
        sourceId = 0;
        creatorId = ownerId; // TODO: preserve creator?
        used = Item.UNUSED;
        location = 0;
    }

    /**
     * Initialize this record so that it actually represents the specified clone.
     */
    public void initFromClone (CloneRecord clone)
    {
        // copy our itemId to parent, and take the clone's itemId
        this.sourceId = this.itemId;
        this.itemId = clone.itemId;

        // clear out our catalog id; clones are never catalog originals
        this.catalogId = 0;

        this.ownerId = clone.ownerId;
        this.used = clone.used;
        this.location = clone.location;
        this.lastTouched = clone.lastTouched;
    }

    /**
     * Build a POJO version of this Record, for use outside the persistence system.
     */
    public Item toItem ()
    {
        Item item = createItem();
        item.itemId = itemId;
        item.sourceId = sourceId;
        item.ownerId = ownerId;
        item.catalogId = catalogId;
        item.rating = rating;
        item.used = used;
        item.location = location;
        item.lastTouched = lastTouched.getTime();
        item.name = name;
        item.description = description;
        item.creatorId = creatorId;
        item.flagged = flagged;
        item.mature = mature;
        item.furniMedia = (furniMediaHash == null) ?
            null : new MediaDesc(furniMediaHash, furniMimeType, furniConstraint);
        item.thumbMedia = (thumbMediaHash == null) ?
            null : new MediaDesc(thumbMediaHash, thumbMimeType, thumbConstraint);
        return item;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Initializes this persistent record from the supplied runtime record.
     */
    protected void fromItem (Item item)
    {
        itemId = item.itemId;
        sourceId = item.sourceId;
        ownerId = item.ownerId;
        catalogId = item.catalogId;
        rating = item.rating;
        creatorId = item.creatorId;
        flagged = item.flagged;
        mature = item.mature;
        used = item.used;
        location = item.location;
        lastTouched = new Timestamp(System.currentTimeMillis());
        name = (item.name == null) ? "" : item.name;
        description = (item.description == null) ? "" : item.description;
        if (item.thumbMedia != null) {
            thumbMediaHash = item.thumbMedia.hash;
            thumbMimeType = item.thumbMedia.mimeType;
            thumbConstraint = item.thumbMedia.constraint;
        }
        if (item.furniMedia != null) {
            furniMediaHash = item.furniMedia.hash;
            furniMimeType = item.furniMedia.mimeType;
            furniConstraint = item.furniMedia.constraint;
        }
    }

    /**
     * Creates an instance of the appropriate item runtime object and initializes the derived
     * class's custom fields. Used by {@link #toItem}.
     */
    protected abstract Item createItem ();
}
