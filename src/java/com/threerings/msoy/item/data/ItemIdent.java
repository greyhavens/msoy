//
// $Id$

package com.threerings.msoy.item.data;

import com.threerings.msoy.item.util.ItemEnum;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemIdent
{
    /** The ItemEnum that represents this item's type. */
    public ItemEnum type;

    /** The integer identifier of the item. */
    public int itemId;

    /**
     * A constructor used for unserialization.
     */
    public ItemIdent ()
    {
    }

    /**
     * Creates an identifier for the specified item.
     */
    public ItemIdent (ItemEnum type, int itemId)
    {
        this.type = type;
        this.itemId = itemId;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return type + ":" + itemId;
    }
}
