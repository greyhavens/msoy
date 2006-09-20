//
// $Id$

package com.threerings.msoy.item.util;

import java.util.zip.CRC32;

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
    PHOTO(Photo.class),
    DOCUMENT(Document.class),
    FURNITURE(Furniture.class),
    GAME(Game.class),

    // NOTE: If you add an item to this list, you need to make sure that
    // Item.getType() returns the string value of the enumeration that is added
    // to correspond to the new item.
    UNUSED(null);

    /**
     * Maps an {@link ItemEnum}'s code code back to an instance.
     */
    public static ItemEnum getItem (int code)
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
    public int getCode ()
    {
        return _code;
    }

    ItemEnum (Class<? extends Item> iclass)
    {
        _itemClass = iclass;
        _code = register(this);
    }

    protected static int register (ItemEnum value)
    {
        // this method gets called before the class's static initializer
        // (magic!) so we have to create these here
        if (_crc == null) {
            _crc = new CRC32();
            _codeToEnum = new HashIntMap<ItemEnum>();
        }

        // compute our unique code
        _crc.reset();
        _crc.update(value.toString().getBytes());
        int code = (int)_crc.getValue();

        if (_codeToEnum.containsKey(code)) {
            log.warning("Item name collision! " + value + " and " +
                _codeToEnum.get(code) + " both map to '" + code + "'.");
        } else {
            _codeToEnum.put(code, value);
        }

        return code;
    }

    /** The unique code for this item. */
    protected int _code;

    /** The derived class for this item. */
    protected Class<? extends Item> _itemClass;

    /** The table mapping codes back to enumerated values. */
    protected static HashIntMap<ItemEnum> _codeToEnum;

    /** Used to compute 32-bit codes for each item type. */
    protected static CRC32 _crc;
}
