//
// $Id$

package com.threerings.msoy.item.web;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemGIdent
    implements IsSerializable
{
    /** The item type identifier. */
    public byte type;

    /** The integer identifier of the item. */
    public int itemId;

    /**
     * A constructor used for unserialization.
     */
    public ItemGIdent ()
    {
    }

    /**
     * Creates an identifier for the specified item.
     */
    public ItemGIdent (byte type, int itemId)
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
