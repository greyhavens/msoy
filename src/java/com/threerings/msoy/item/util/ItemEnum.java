//
// $Id$

package com.threerings.msoy.item.util;

import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;

/**
 * Defines the set of all known concrete {@link Item} types.
 */
public enum ItemEnum
{
    // NOTE WELL: These enumerations must never change order nor be removed;
    // all new item mappings *MUST* go at the end of this list (before UNUSED).
    // Don't fuck up!
    PHOTO(Photo.class),
    DOCUMENT(Document.class),
    FURNITURE(Furniture.class),
    GAME(Game.class),

    // NOTE ALSO: If you add an item to this list, you need to make sure that
    // Item.getType() returns the string value of the enumeration that is added
    // to correspond to the new item.
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
