//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.common.primitives.Doubles;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnum;

import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.MediaMimeTypes;

/**
 * The base class for all digital items in the MSOY system.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly limited in their contents
 * as they must be translatable into JavaScript ({@link IsSerializable}) and must work with the
 * Presents streaming system ({@link Streamable}).
 */
public abstract class Item implements Comparable<Item>, Streamable, IsSerializable, DSet.Entry
{
    /**
     * Enumerates the ways in which an item can be used, stored in the {@link #used} member.
     */
    public enum UsedAs
        implements ByteEnum
    {
        /** Indicates that the item is unused. */
        NOTHING(0),

        /** Indicates that the item is placed as furniture. The 'location' field will contain the
         * sceneId. */
        FURNITURE(1),

        /** Indicates that the item is used as an avatar. */
        AVATAR(2),

        /** Indicates that the item is used as a pet let out in a room. The 'location' field will
         * contain the sceneId.*/
        PET(3),

        /** Indicates that the item is used in a scene as background bitmap or music (as
         * appropriate). The 'location' field will contain the sceneId. */
        BACKGROUND(4);

        // from ByteEnum
        public byte toByte ()
        {
            return _value;
        }

        public boolean forAnything ()
        {
            return this != NOTHING;
        }

        UsedAs (int value)
        {
            _value = (byte)value;
        }

        protected byte _value;
    }

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String FURNI_MEDIA = "furni";

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String THUMB_MEDIA = "thumb";

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String MAIN_MEDIA = "main";

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String AUX_MEDIA = "aux";

    /** Indicates that this item is a "remixed clone", that we can revert to the original mix. */
    public static final byte ATTR_REMIXED_CLONE = 1 << 0;

    /** Indicates that this item is a remixable clone and the original version is updated. */
    public static final byte ATTR_ORIGINAL_UPDATED = 1 << 1;

    /** Indicates that this item is stamped for the current user's theme, where relevant. */
    public static final byte ATTR_THEME_STAMPED = 1 << 2;

    /** The maximum length for item names. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The maximum length for item descriptions. */
    public static final int MAX_DESCRIPTION_LENGTH = 200;

    // == Instance variables follow =========================================

    /** This item's unique identifier. <em>Note:</em> this identifier is not globally unique among
     * all digital items. Each type of item has its own identifier space. */
    public int itemId;

    /** The item id from which this object was cloned, or 0 if this is not a clone. */
    public int sourceId;

    /** A bit-mask of runtime attributes about this item. These are not saved in the database
     * anywhere, these are created on-the-fly when looking at metadata not otherwise sent
     * down from the server. */
    public byte attrs;

    /** The member id of the member that created this item. */
    public int creatorId;

    /** The member id of the member that owns this item, or 0 if it's not in any inventory;
     * e.g. it's listed in the catalog or a gifted item in a mail message. */
    public int ownerId;

    /** The id of the catalog listing associated with this item. 0 if the item was never listed,
     * negative if once listed, positive if listed currently. This item may be the
     * original item of a purchased clone. Use isListedOriginal() to check. */
    public int catalogId;

    /** The current sum of all ratings that have been applied to this item. */
    public int ratingSum;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** Indicates where this item is being used. */
    public UsedAs used = UsedAs.NOTHING;

    /** A number, interpreted along with 'used' that identifies the location at which this item is
     * being used. */
    public int location;

    /** Our last-touched timestamp, expressed as a double (for actionscript compat, groan). */
    public double lastTouched;

    /** The user provided name for this item. */
    public String name;

    /** The user provided description for this item. */
    public String description;

    /** Whether or not this item represents mature content. */
    public boolean mature;

    /**
     * Returns a {@link MediaDesc} configured to display the default furniture media for items of
     * the specified type.
     */
    public static MediaDesc getDefaultFurniMediaFor (MsoyItemType itemType)
    {
        return new DefaultItemMediaDesc(MediaMimeTypes.IMAGE_PNG, itemType, FURNI_MEDIA);
    }

    /**
     * Returns a {@link MediaDesc} configured to display the default thumbnaiul media for items of
     * the specified type.
     */
    public static MediaDesc getDefaultThumbnailMediaFor (MsoyItemType itemType)
    {
        return new DefaultItemMediaDesc(MediaMimeTypes.IMAGE_PNG, itemType, THUMB_MEDIA,
                                        MediaDesc.HALF_VERTICALLY_CONSTRAINED);
    }

    /**
     * Get the translation key for the specified item type.
     */
    public static String getTypeKey (MsoyItemType type)
    {
        return "m." + type.typeName();
    }

    /**
     * Returns this item's composite identifier.
     */
    public ItemIdent getIdent ()
    {
        return new ItemIdent(getType(), itemId);
    }

    /**
     * Gets the type code for this item.
     */
    public abstract MsoyItemType getType ();

    /**
     * Get the translation key for this type of item.
     */
    public String getTypeKey ()
    {
        return getTypeKey(getType());
    }

    /**
     * Returns the id of the item of which this item is a clone, or this item's own item id if it
     * is an original item.
     */
    public int getMasterId ()
    {
        return (sourceId == 0) ? itemId : sourceId;
    }

    /**
     * Returns true if this item is a catalog original, rather than just a clone of something
     * listed in the catalog.
     */
    public boolean isListedOriginal ()
    {
        return (sourceId == 0) && (catalogId > 0) && (ownerId != 0);
    }

    /**
     * Returns true if this item is a catalog master from which clones are configured,
     * rather than just a clone, or the original item.
     */
    public boolean isCatalogMaster ()
    {
        return (sourceId == 0) && (catalogId > 0) && (ownerId == 0);
    }

    /**
     * Returns true if this item is a clone of something listed in the catalog.
     */
    public boolean isCatalogClone ()
    {
        return (sourceId != 0) && (catalogId > 0);
    }

    /**
     * Tests whether a given attribute is set on this item.
     */
    public boolean isAttrSet (byte attr)
    {
        return (attrs & attr) != 0;
    }

    /**
     * Is the item currently in use somewhere?
     */
    public boolean isUsed ()
    {
        return (used != UsedAs.NOTHING);
    }

    /**
     * Returns true if this item can be sold in the catalog, false otherwise.
     */
    public boolean isSalable ()
    {
        return true;
    }

    /**
     * Returns true if this item can be rated (it is a clone of a catalog entry, or a catalog entry
     * itself), false if not.
     */
    public boolean isRatable ()
    {
        return (sourceId != 0) || (sourceId == 0 && ownerId == 0);
    }

    /**
     * Calculate this item's average rating from the sum and count.
     */
    public float getRating ()
    {
        return (ratingCount > 0) ? (float) ratingSum / ratingCount : 0f;
    }

    /**
     * Returns the media that should be shown when previewing this item.
     */
    public abstract MediaDesc getPreviewMedia ();

    /**
     * Returns a media descriptor for the media that should be used to display our thumbnail
     * representation.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return (_thumbMedia == null) ? getDefaultThumbnailMedia() : _thumbMedia;
    }

    /**
     * Returns our raw thumbnail media which may be null. Don't call this method.
     */
    public MediaDesc getRawThumbnailMedia ()
    {
        return _thumbMedia;
    }

    /**
     * Configures this item's thumbnail media.
     */
    public void setThumbnailMedia (MediaDesc thumbMedia)
    {
        _thumbMedia = thumbMedia;
    }

    /**
     * Returns a media descriptor for the media that should be used to display our furniture
     * representation.
     */
    public MediaDesc getFurniMedia ()
    {
        return (_furniMedia == null) ? getDefaultFurniMedia() : _furniMedia;
    }

    /**
     * Returns our raw furniture media which may be null. Don't call this method.
     */
    public MediaDesc getRawFurniMedia ()
    {
        return _furniMedia;
    }

    /**
     * Configures this item's furniture media.
     */
    public void setFurniMedia (MediaDesc furniMedia)
    {
        _furniMedia = furniMedia;
    }

    /**
     * Return the "primary" media for this item. Don't do any funny business,
     * return null if the primary media is null and overridden by something
     * else.
     */
    public MediaDesc getPrimaryMedia ()
    {
        return _furniMedia;
    }

    /**
     * Update the primary media, usually as a result of remixing.
     */
    public void setPrimaryMedia (MediaDesc desc)
    {
        _furniMedia = desc;
    }

    /**
     * Called during item creation to ensure that media descriptors are not duplicated in this item
     * more than necessary.
     */
    public void checkConsolidateMedia ()
    {
        if (_thumbMedia != null && getThumbnailMedia().equals(getDefaultThumbnailMedia())) {
            _thumbMedia = null;
        }
        if (_furniMedia != null && getFurniMedia().equals(getDefaultFurniMedia())) {
            _furniMedia = null;
        }
    }

    /**
     * Verifies that all the required fields in this particular Item subclass are filled in, make
     * sense, and are consistent with each other. This is used to verify the data being edited by a
     * user during item creation, and also that the final uploaded item isn't hacked.
     */
    public boolean isConsistent ()
    {
        return getPrimaryMedia() != null;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return getIdent();
    }

    @Override // from Object
    public int hashCode ()
    {
        return itemId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof Item) {
            Item that = (Item) other;
            // cheap comparison first...
            return (this.itemId == that.itemId) && (this.getType() == that.getType());
        }
        return false;
    }

    // from Comparable<Item>
    public int compareTo (Item other)
    {
        return Doubles.compare(other.lastTouched, lastTouched); // reversed
    }

    @Override // from Object
    public String toString ()
    {
        return "[type=" + getType().typeName() + ", id=" + itemId + ", name=" + name + "]";
    }

    /**
     * Returns the default thumbnail media for use if this item has no provided custom media.
     */
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        return getDefaultThumbnailMediaFor(getType());
    }

    /**
     * Returns the default furni media for use if this item has no provided custom media.
     */
    protected MediaDesc getDefaultFurniMedia ()
    {
        return getDefaultFurniMediaFor(getType());
    }

    /**
     * A handy method that makes sure that the specified text is not null or all-whitespace and is
     * less than or equal to the specified maximum length.  Usually used by isConsistent().
     */
    protected static boolean nonBlank (String text, int maxLength)
    {
        text = (text == null) ? "" : text.trim();
        return (text.length() > 0 && text.length() <= maxLength);
    }

    protected static boolean supports (byte itemType, byte[] supportedTypes)
    {
        for (byte type : supportedTypes) {
            if (type == itemType) {
                return true;
            }
        }
        return false;
    }

    /** The media used to display this item's thumbnail representation. */
    protected MediaDesc _thumbMedia;

    /** The media used to display this item's furniture representation. */
    protected MediaDesc _furniMedia;

}
