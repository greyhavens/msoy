//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.threerings.io.Streamable;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;

/**
 * The base class for all digital items in the MSOY system.
 */
@Entity
@Table
public abstract class ItemRecord implements Streamable, Cloneable
{
    public static final int BASE_SCHEMA_VERSION = 1;

    public static final String ITEM_ID = "itemId";
    public static final String PARENT_ID = "parentId";
    public static final String FLAGS = "flags";
    public static final String CREATOR_ID = "creatorId";
    public static final String OWNER_ID = "ownerId";
    public static final String RATING = "rating";
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
        }
        throw new RuntimeException("Unknown item type: " + item);
    }
    
    /** This item's unique identifier. <em>Note:</em> this identifier is not
     * globally unique among all digital items. Each type of item has its own
     * identifier space. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE)
    public int itemId;

    /**
     * The item ID from which this object was cloned, or -1 if this is not
     * a clone. This field is not persisted to the database, but set when
     * we load a clone.
     */
    @Transient
    public int parentId = -1;
    
    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    @Column(nullable=false)
    public byte flags;

    /** The member id of the member that created this item. */
    @Column(nullable=false)
    public int creatorId;

    /**
     *  The member id of the member that owns this item, or -1 if the item
     *  is an immutable catalog listing.
     */
    @Column(nullable=false)
    public int ownerId;

    /** The current rating of this item, from 1 to 5. */
    @Column(nullable=false)
    public float rating;
    
    /** A hash code identifying the media used to display this item's thumbnail
     * representation. */
    @Column(nullable=true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    @Column(nullable=true)
    public byte thumbMimeType;

    /** A hash code identifying the media used to display this item's furniture
     * representation. */
    @Column(nullable=true)
    public byte[] furniMediaHash;

    /**
     * The MIME type of the  {@link #furniMediaHash}  media. 
     */
    @Column(nullable = true)
    public byte furniMimeType;

    public ItemRecord ()
    {
        super();
    }
    
    protected ItemRecord (Item item)
    {
        super();
        
        this.creatorId = item.creatorId;
        this.flags = item.flags;
        this.furniMediaHash = item.furniMediaHash == null ?
            null : item.furniMediaHash.clone();
        this.furniMimeType = item.furniMimeType;
        this.itemId = item.itemId;
        this.ownerId = item.ownerId;
        this.parentId = item.parentId;
        this.rating = item.rating;
        this.thumbMediaHash = item.thumbMediaHash == null ?
            null : item.thumbMediaHash.clone();
        this.thumbMimeType = item.thumbMimeType;
    }
    

    /**
     * This is used to map {@link ItemRecord} concrete classes to ItemEnum
     * values.
     */
    public abstract ItemEnum getType ();

    @Override
    public Object clone ()
    {
        try {
            ItemRecord clone = (ItemRecord) super.clone();
            clone.furniMediaHash = furniMediaHash.clone();
            clone.thumbMediaHash = thumbMediaHash.clone();
            return clone;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // not going to happen
        }
    }
    
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
            return (this.itemId == that.itemId) &&
                (this.getType() == that.getType());
        }
        return false;
    }
    
    public Item toItem ()
    {
        Item item = createItem();
        item.creatorId = this.creatorId;
        item.flags = this.flags;
        item.furniMediaHash = this.furniMediaHash == null ?
            null : this.furniMediaHash.clone();
        item.furniMimeType = this.furniMimeType;
        item.itemId = this.itemId;
        item.ownerId = this.ownerId;
        item.parentId = this.parentId;
        item.rating = this.rating;
        item.thumbMediaHash = this.thumbMediaHash == null ?
            null : this.thumbMediaHash.clone();
        item.thumbMimeType = this.thumbMimeType;
        return item;
    }
    
    protected abstract Item createItem ();
}
