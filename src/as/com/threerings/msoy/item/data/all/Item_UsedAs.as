
package com.threerings.msoy.item.data.all {

import com.threerings.util.Enum;

/**
 * Enumerates the ways in which an item can be used, stored in the <code>Item.used</code> member.
 */
public final class Item_UsedAs extends Enum
{
    /** Indicate that the item is unused. */
    public static const NOTHING :Item_UsedAs = new Item_UsedAs("NOTHING", 0);

    /** Indicates that the item is placed as furniture. The 'location' field will contain the
     * sceneId. */
    public static const FURNITURE :Item_UsedAs = new Item_UsedAs("FURNITURE", 1);

    /** Indicates that the item is used as an avatar. */
    public static const AVATAR :Item_UsedAs = new Item_UsedAs("AVATAR", 2);

    /** Indicates that the item is used as a pet let out in a room. The 'location' field will
     * contain the sceneId.*/
    public static const PET :Item_UsedAs = new Item_UsedAs("PET", 3);

    /** Indicates that the item is used in a scene as background bitmap or music (as
     * appropriate). The 'location' field will contain the sceneId. */
    public static const BACKGROUND :Item_UsedAs = new Item_UsedAs("BACKGROUND", 4);

    finishedEnumerating(Item_UsedAs);

    /** @private */
    public function Item_UsedAs (name :String, value :int)
    {
        super(name);
        _value = value;
    }

    public function forAnything () :Boolean
    {
        return this != NOTHING;
    }

    public function toInt () :int
    {
        return _value;
    }

    public static function valueOf (name :String) :Item_UsedAs
    {
        return Enum.valueOf(Item_UsedAs, name) as Item_UsedAs;
    }

    public static function values () :Array
    {
        return Enum.values(Item_UsedAs);
    }

    protected var _value :int;
}

}
