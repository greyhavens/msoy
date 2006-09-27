//
// $Id$

package com.threerings.msoy.item.util {

import flash.errors.IllegalOperationError;

import com.threerings.util.ClassUtil;
import com.threerings.util.Hashtable;

import com.threerings.msoy.item.web.Document; 
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;

public final class ItemEnum
{
    private static var _blockConstructor :Boolean = false;
    // NOTE: each new type added should have a new code. Do not re-use
    // old codes.
    public static const PHOTO :ItemEnum = new ItemEnum(Photo, 1);
    public static const DOCUMENT :ItemEnum = new ItemEnum(Document, 2);
    public static const FURNITURE :ItemEnum = new ItemEnum(Furniture, 3);
    public static const GAME :ItemEnum = new ItemEnum(Game, 4);
    _blockConstructor = true; // we emulate real enums..

    /**
     * Maps an {@link ItemEnum}'s code code back to an instance.
     */
    public static function getItem (code :int) :ItemEnum
    {
        return (_codeToEnum.get(code) as ItemEnum);
    }

    /**
     * Returns the {@link Item} derived class that represents this item type.
     */
    public function getItemClass () :Class
    {
        return _itemClass;
    }

    /**
     * Returns the unique code for this item type, which is a function of its
     * name.
     */
    public function getCode () :int
    {
        return _code;
    }

    /**
     * Constructor.
     */
    public function ItemEnum (iclass :Class, code :int)
    {
        if (_blockConstructor) {
            throw new IllegalOperationError();

        } else if (_codeToEnum == null) {
            _codeToEnum = new Hashtable();
        }

        _itemClass = iclass;
        _code = code;

        if (_codeToEnum.containsKey(code)) {
            Log.getLog(this).warning("Item code collision! " + this + " and " + 
                _codeToEnum.get(code) + " both use code '" + code + "'.");
        } else {
            _codeToEnum.put(code, this);
        }
    }

    public function toString () :String
    {
        return "ItemEnum(" + ClassUtil.tinyClassName(_itemClass) + ", " +
            _code + ")";
    }

    /** The unique code for this item. */
    protected var _code :int;

    /** The derived class for this item. */
    protected var _itemClass :Class;

    /** The table mapping codes back to enumerated values. */
    protected static var _codeToEnum :Hashtable;
}
}
