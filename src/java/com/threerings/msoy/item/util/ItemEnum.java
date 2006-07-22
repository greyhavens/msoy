//
// $Id$

package com.threerings.msoy.item.util;

import com.threerings.msoy.item.data.Document;
import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.Photo;

/**
 * Defines the set of all known concrete {@link Item} types.
 */
public enum ItemEnum
{
    // NOTE WELL: These enumerations never change order nor be removed; all new
    // item mappings *MUST* go at the end of this list (before UNUSED). Don't
    // fuck up!
    PHOTO(Photo.class),
    DOCUMENT(Document.class),

    // NOTE ALSO: if you add an item to this list, you need to make sure that
    // Item.getType() returns the string value of the enumeration that is added
    // to correspond to the new item
    UNUSED(null);

    /**
     * Returns the {@link Item} derived class that represents this item type.
     */
    public Class<? extends Item> getItemClass ()
    {
        return _itemClass;
    }

    ItemEnum (Class<? extends Item> iclass)
    {
        _itemClass = iclass;
    }

    protected Class<? extends Item> _itemClass;
}
