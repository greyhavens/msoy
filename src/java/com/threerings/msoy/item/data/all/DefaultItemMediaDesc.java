//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.StaticMediaDesc;

/**
 * Provides static media for items.
 */
public class DefaultItemMediaDesc extends StaticMediaDesc
{
    /** Used for unserialization. */
    public DefaultItemMediaDesc ()
    {
    }

    /**
     * Creates a configured static media descriptor.
     */
    public DefaultItemMediaDesc (byte mimeType, byte itemType, String mediaType)
    {
        this(mimeType, itemType, mediaType, NOT_CONSTRAINED);
    }

    /**
     * Creates a configured static media descriptor.
     */
    public DefaultItemMediaDesc (byte mimeType, byte itemType, String mediaType, byte constraint)
    {
        super(mimeType, Item.getTypeName(itemType), mediaType, constraint);
        _itemTypeCode = itemType;
    }

    /**
     * Returns the type of item for which we're providing static media.
     */
    public byte getItemTypeCode ()
    {
        return _itemTypeCode;
    }

    protected byte _itemTypeCode;
}
