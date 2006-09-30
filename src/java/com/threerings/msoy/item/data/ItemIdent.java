//
// $Id$

package com.threerings.msoy.item.data;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemIdent
{
    /** The type constant that represents this item's type. */
    public byte type;

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
    public ItemIdent (byte type, int itemId)
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
