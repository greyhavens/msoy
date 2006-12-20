//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;
import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.Photo;

/**
 * The base class for all digital items in the MSOY system.
 */
@Entity
@Table
public abstract class ItemRecord implements Streamable, Cloneable
{
    public static final int BASE_SCHEMA_VERSION = 5;

    public static final String ITEM_ID = "itemId";
    public static final String PARENT_ID = "parentId";
    public static final String FLAGS = "flags";
    public static final String CREATOR_ID = "creatorId";
    public static final String OWNER_ID = "ownerId";
    public static final String RATING = "rating";
    public static final String USED = "used";
    public static final String LOCATION = "location";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String THUMB_MEDIA_HASH = "thumbMediaHash";
    public static final String THUMB_MIME_TYPE = "thumbMimeType";
    public static final String FURNI_MEDIA_HASH = "furniMediaHash";
    public static final String FURNI_MIME_TYPE = "furniMimeType";

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
        }
        throw new RuntimeException("Unknown item type: " + item);
    }

    /** This item's unique identifier. <em>Note:</em> this identifier is not
     * globally unique among all digital items. Each type of item has its own
     * identifier space. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE)
    public int itemId;

    /** The item ID from which this object was cloned, or -1 if this is not a clone. This field is
     * not persisted to the database, but set when we load a clone. */
    @Computed(required=false)
    public int parentId = -1;

    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    public byte flags;

    /** The member id of the member that created this item. */
    public int creatorId;

    /** The member id of the member that owns this item, or -1 if the item
     * is an immutable catalog listing. */
    public int ownerId;

    /** The current rating of this item, from 1 to 5. */
    public float rating;

    /** How this item is being used (see Item.USED_AS_FURNITURE). */
    public byte used;

    /** Where it's being used. */
    public int location;

    /** A user supplied name for this item. */
    public String name;

    /** A user supplied description for this item. */
    public String description;

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
        flags = item.flags;
        used = item.used;
        location = item.location;
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
     * Clears out any fields that should be reset when listing this item in the catalog.
     */
    public void clearForListing ()
    {
        ownerId = -1;
        itemId = 0;
        used = 0;
        location = 0;
    }

    public Item toItem ()
    {
        Item item = createItem();
        item.itemId = itemId;
        item.ownerId = ownerId;
        item.parentId = parentId;
        item.rating = rating;
        item.used = used;
        item.location = location;
        item.name = name;
        item.description = description;
        item.creatorId = creatorId;
        item.flags = flags;
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
