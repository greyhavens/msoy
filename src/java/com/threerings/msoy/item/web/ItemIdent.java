//
// $Id$

package com.threerings.msoy.item.web;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemIdent
    implements IsSerializable
{
    /** The item type identifier. */
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

    // @Override from Object
    public boolean equals (Object other)
    {
        if (other instanceof ItemIdent) {
            ItemIdent that = (ItemIdent) other;
            return (this.type == that.type) && (this.itemId == that.itemId);
        }
        return false;
    }

    // @Override from Object
    public int hashCode ()
    {
        return (type * 37) | itemId;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return type + ":" + itemId;
    }
}
