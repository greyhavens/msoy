//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Table;

import com.samskivert.util.StringUtil;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Document;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.data.all.Toy;
import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Decor;

/**
 * The base class for all digital items in the MSOY system.
 */
@Table
@Entity(indices={
    @Index(name="locationIndex", columns={ ItemRecord.LOCATION } ),
    @Index(name="ixFlagged", columns={ ItemRecord.FLAGGED } ),
    @Index(name="ixMature", columns={ ItemRecord.MATURE } ),
    @Index(name="ixOwner", columns={ ItemRecord.OWNER_ID }),
    @Index(name="ixCreator", columns={ ItemRecord.CREATOR_ID })
})
public abstract class ItemRecord extends PersistentRecord implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(ItemRecord.class, ITEM_ID);

    /** The column identifier for the {@link #parentId} field. */
    public static final String PARENT_ID = "parentId";

    /** The qualified column identifier for the {@link #parentId} field. */
    public static final ColumnExp PARENT_ID_C =
        new ColumnExp(ItemRecord.class, PARENT_ID);

    /** The column identifier for the {@link #flagged} field. */
    public static final String FLAGGED = "flagged";

    /** The qualified column identifier for the {@link #flagged} field. */
    public static final ColumnExp FLAGGED_C =
        new ColumnExp(ItemRecord.class, FLAGGED);

    /** The column identifier for the {@link #creatorId} field. */
    public static final String CREATOR_ID = "creatorId";

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(ItemRecord.class, CREATOR_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(ItemRecord.class, OWNER_ID);

    /** The column identifier for the {@link #rating} field. */
    public static final String RATING = "rating";

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(ItemRecord.class, RATING);

    /** The column identifier for the {@link #used} field. */
    public static final String USED = "used";

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(ItemRecord.class, USED);

    /** The column identifier for the {@link #location} field. */
    public static final String LOCATION = "location";

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(ItemRecord.class, LOCATION);

    /** The column identifier for the {@link #lastTouched} field. */
    public static final String LAST_TOUCHED = "lastTouched";

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(ItemRecord.class, LAST_TOUCHED);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(ItemRecord.class, NAME);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(ItemRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #mature} field. */
    public static final String MATURE = "mature";

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(ItemRecord.class, MATURE);

    /** The column identifier for the {@link #thumbMediaHash} field. */
    public static final String THUMB_MEDIA_HASH = "thumbMediaHash";

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(ItemRecord.class, THUMB_MEDIA_HASH);

    /** The column identifier for the {@link #thumbMimeType} field. */
    public static final String THUMB_MIME_TYPE = "thumbMimeType";

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(ItemRecord.class, THUMB_MIME_TYPE);

    /** The column identifier for the {@link #thumbConstraint} field. */
    public static final String THUMB_CONSTRAINT = "thumbConstraint";

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(ItemRecord.class, THUMB_CONSTRAINT);

    /** The column identifier for the {@link #furniMediaHash} field. */
    public static final String FURNI_MEDIA_HASH = "furniMediaHash";

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(ItemRecord.class, FURNI_MEDIA_HASH);

    /** The column identifier for the {@link #furniMimeType} field. */
    public static final String FURNI_MIME_TYPE = "furniMimeType";

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(ItemRecord.class, FURNI_MIME_TYPE);

    /** The column identifier for the {@link #furniConstraint} field. */
    public static final String FURNI_CONSTRAINT = "furniConstraint";

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(ItemRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int BASE_SCHEMA_VERSION = 10;
    public static final int BASE_MULTIPLIER = 1000;

    public static ItemRecord newRecord (Item item) {
        if (item instanceof Document) {
            return new DocumentRecord((Document) item);
        } else if (item instanceof Furniture) {
            return new FurnitureRecord((Furniture) item);
        } else if (item instanceof Game) {
            return new GameRecord((Game) item);
        } else if (item instanceof Photo) {
            return new PhotoRecord((Photo) item);
        } else if (item instanceof Avatar) {
            return new AvatarRecord((Avatar) item);
        } else if (item instanceof Pet) {
            return new PetRecord((Pet) item);
        } else if (item instanceof Audio) {
            return new AudioRecord((Audio) item);
        } else if (item instanceof Video) {
            return new VideoRecord((Video) item);
        } else if (item instanceof Decor) {
            return new DecorRecord((Decor) item);
        } else if (item instanceof Toy) {
            return new ToyRecord((Toy) item);
        }
        throw new RuntimeException("Unknown item type: " + item);
    }

    /** This item's unique identifier. <em>Note:</em> this identifier is not
     * globally unique among all digital items. Each type of item has its own
     * identifier space. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE, allocationSize=1)
    public int itemId;

    /** The item ID from which this object was cloned, or 0 if this is not a clone. This field is
     * not persisted to the database, but set when we load a clone. */
    @Computed(required=false)
    public int parentId = 0;

    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    public byte flagged;

    /** The member id of the member that created this item. */
    public int creatorId;

    /** The member id of the member that owns this item, or 0 if it's not in any inventory;
     * e.g. it's listed in the catalog or a gifted item in a mail message. */
    public int ownerId;

    /** The current rating of this item, from 1 to 5. */
    public float rating;

    /** How this item is being used (see Item.USED_AS_FURNITURE). */
    public byte used;

    /** Where it's being used. */
    public int location;

    /** The timestamp at which this item was last touched. */
    public Timestamp lastTouched;

    /** A user supplied name for this item. */
    public String name;

    /** A user supplied description for this item. */
    public String description;

    /** Whether or not this item represents mature content. */
    public boolean mature;

    /** A hash code identifying the media used to display this item's thumbnail
     * representation. */
    @Column(nullable=true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** The size constraint on the {@link #thumbMediaHash} media. */
    public byte thumbConstraint;

    /** A hash code identifying the media used to display this item's furniture
     * representation. */
    @Column(nullable=true)
    public byte[] furniMediaHash;

    /** The MIME type of the {@link #furniMediaHash} media. */
    public byte furniMimeType;

    /** The size constraint on the {@link #furniMediaHash} media. */
    public byte furniConstraint;

    public ItemRecord ()
    {
        super();
    }

    protected ItemRecord (Item item)
    {
        super();

        itemId = item.itemId;
        ownerId = item.ownerId;
        parentId = item.parentId;
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
     */
    public void clearForListing ()
    {
        ownerId = 0;
        itemId = 0;
        used = 0;
        location = 0;
    }

    /**
     * Initialize this record so that it actually represents the specified clone.
     */
    public void initFromClone (CloneRecord clone)
    {
        // copy our itemId to parent, and take the clone's itemId
        this.parentId = this.itemId;
        this.itemId = clone.itemId;

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
        item.ownerId = ownerId;
        item.parentId = parentId;
        item.rating = rating;
        item.used = used;
        item.location = location;
        item.lastTouched = (double) lastTouched.getTime();
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

    protected abstract Item createItem ();

}
