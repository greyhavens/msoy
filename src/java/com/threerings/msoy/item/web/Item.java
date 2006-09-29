//
// $Id$

package com.threerings.msoy.item.web;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * The base class for all digital items in the MSOY system.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly
 * limited in their contents as they must be translatable into JavaScript
 * ({@link IsSerializable}) and must work with the Presents streaming system
 * ({@link Streamable}) and must work with the JORA object persistence system.
 */
public abstract class Item implements Streamable, IsSerializable
{
    /** This item's unique identifier. <em>Note:</em> this identifier is not
     * globally unique among all digital items. Each type of item has its own
     * identifier space. */
    public int itemId;

    /** The item ID from which this object was cloned, or -1 if this is not
     * a clone. */
    public int parentId = -1;
    
    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    public byte flags;

    /** The member id of the member that created this item. */
    public int creatorId;

    /** The member id of the member that owns this item, or -1 if the item
     *  is an immutable catalog listing. */
    public int ownerId;

    /** The current rating of this item, either 0 or between 1 and 5. */
    public float rating;
    
    /** A hash code identifying the media used to display this item's thumbnail
     * representation. */
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** A hash code identifying the media used to display this item's furniture
     * representation. */
    public byte[] furniMediaHash;

    /** The MIME type of the {@link #furniMediaHash} media. */
    public byte furniMimeType;

    /**
     * Returns this item's composite identifier.
     */
    public ItemGIdent getIdent ()
    {
        return new ItemGIdent(getType(), itemId);
    }

    /**
     * This is used to map {@link Item} concrete classes to ItemEnum values. We
     * cannot simply reference the ItemEnum itself because item classes must be
     * translatable to JavaScript which doesn't support enums. So be sure to
     * properly wire things up when creating a new concrete item class.
     */
    public abstract String getType ();

    /**
     * Get a textual description of this item.
     */
    public abstract String getDescription ();

    /**
     * Returns the parentId, or the itemId if this item is not a clone.
     */
    public int getProgenitorId ()
    {
        return (parentId == -1) ? itemId : parentId;
    }

    /**
     * Returns a media descriptor for the media that should be used to display
     * our thumbnail representation.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return (thumbMediaHash == null) ? getDefaultThumbnailMedia() :
            new MediaDesc(thumbMediaHash, thumbMimeType);
    }

    /**
     * Returns a media descriptor for the media that should be used to display
     * our furniture representation.
     */
    public MediaDesc getFurniMedia ()
    {
        return (furniMediaHash == null) ? getDefaultFurniMedia() :
            new MediaDesc(furniMediaHash, furniMimeType);
    }

    /**
     * Returns the path to a thumbnail image for this item.
     */
    public String getThumbnailPath ()
    {
        return getThumbnailMedia().getMediaPath();
    }

    /**
     * Called during item creation to ensure that media descriptors
     * are not duplicated in this item more than necessary.
     */
    public void checkConsolidateMedia ()
    {
        if (thumbMediaHash != null &&
                getThumbnailMedia().equals(getDefaultThumbnailMedia())) {
            thumbMediaHash = null;
            thumbMimeType = (byte) 0;
        }

        if (furniMediaHash != null &&
                getFurniMedia().equals(getDefaultFurniMedia())) {
            furniMediaHash = null;
            furniMimeType = (byte) 0;
        }
    }

    /**
     * Verify that all the required fields in this particular Item subclass are
     * filled in, make sense, and are consistent with each other. This is used
     * to verify the data being edited by a user during item creation, and also
     * that the final uploaded item isn't hacked.
     */
    public boolean isConsistent ()
    {
        return true;
    }

    // @Override
    public int hashCode ()
    {
        return itemId;
    }

    // @Override
    public boolean equals (Object other)
    {
        if (other instanceof Item) {
            Item that = (Item) other;
            // cheap comparison first...
            return (this.itemId == that.itemId) &&
                this.getType().equals(that.getType());
        }
        return false;
    }

    /**
     * Returns the default thumbnail media for use if this item has no provided
     * custom media.
     */
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        return new StaticMediaDesc(StaticMediaDesc.THUMBNAIL, getType());
    }

    /**
     * Returns the default furni media for use if this item has no provided
     * custom media.
     */
    protected MediaDesc getDefaultFurniMedia ()
    {
        return new StaticMediaDesc(StaticMediaDesc.FURNI, getType());
    }

    /**
     * A handy method that makes sure that the specified text is not null or
     * all-whitespace. Usually used by isConsistent().
     */
    protected static boolean nonBlank (String text)
    {
        return (text != null) && (text.trim().length() > 0);
    }
}
