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

    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    public byte flags;

    /** The member id of the member that owns this item. */
    public int ownerId;

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
     * Returns the path to a thumbnail image for this item.
     */
    public String getThumbnailPath ()
    {
        return "/media/static/items/" + getType().toLowerCase() + ".png";
    }

    /**
     * Verify that all the required fields in this particular Item subclass
     * are filled in, make sense, and are consistent with each other.
     * This is used to verify the data being edited by a user during
     * item creation, and also that the final uploaded item isn't hacked.
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
     * A handy method that makes sure that the specified text is
     * not null or all-whitespace. Usually used by isConsistent().
     */
    protected boolean nonBlank (String text)
    {
        return (text != null) && (text.trim().length() > 0);
    }
}
