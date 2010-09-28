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
    public DefaultItemMediaDesc (byte mimeType, MsoyItemType itemType, String mediaType)
    {
        this(mimeType, itemType, mediaType, NOT_CONSTRAINED);
    }

    /**
     * Creates a configured static media descriptor.
     */
    public DefaultItemMediaDesc (byte mimeType, MsoyItemType itemType, String mediaType, byte constraint)
    {
        super(mimeType, itemType.typeName(), mediaType, constraint);
        _itemType = itemType;
    }

    /**
     * Returns the type of item for which we're providing static media.
     */
    public MsoyItemType getItemTypeCode ()
    {
        return _itemType;
    }

    protected MsoyItemType _itemType;
}
