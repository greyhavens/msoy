//
// $Id$

package com.threerings.msoy.item.util;

import com.samskivert.util.HashIntMap;

import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;

import static com.threerings.msoy.Log.log;

/**
 * Defines the set of all known concrete {@link Item} types.
 */
public enum ItemEnum
{
    // NOTE: Don't remove or change the name of any of these unless you know
    // what you're doing.
    PHOTO(Photo.class, (byte)1),
    DOCUMENT(Document.class,(byte)2),
    FURNITURE(Furniture.class, (byte)3),
    GAME(Game.class, (byte)4),

    // NOTE: If you add an item to this list, you need to make sure that
    // Item.getType() returns the string value of the enumeration that is added
    // to correspond to the new item.
    UNUSED(null, (byte)-1);

    /**
     * Maps an {@link ItemEnum}'s code code back to an instance.
     */
    public static ItemEnum getItem (byte code)
    {
        return _codeToEnum.get(code);
    }

    /**
     * Returns the {@link Item} derived class that represents this item type.
     */
    public Class<? extends Item> getItemClass ()
    {
        return _itemClass;
    }

    /**
     * Returns the unique code for this item type, which is a function of its
     * name.
     */
    public byte getCode ()
    {
        return _code;
    }

    ItemEnum (Class<? extends Item> iclass, byte code)
    {
        _itemClass = iclass;
        _code = code;
    }

    /** The unique code for this item. */
    protected byte _code;

    /** The derived class for this item. */
    protected Class<? extends Item> _itemClass;

    /** The table mapping codes back to enumerated values. */
    protected static HashIntMap<ItemEnum> _codeToEnum =
        new HashIntMap<ItemEnum>();

    // initialize the reverse-mapping
    static {
        for (ItemEnum value : ItemEnum.values()) {
            byte code = value.getCode();
            if (_codeToEnum.containsKey(code)) {
                log.warning("Item code collision! " + value + " and " +
                    _codeToEnum.get(code) + " both use code '" + code + "'.");
            } else {
                _codeToEnum.put(code, value);
            }
        }
    }
}
