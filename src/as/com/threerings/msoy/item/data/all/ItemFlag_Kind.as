//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.Enum;

public final class ItemFlag_Kind extends Enum
{
    public static const MATURE :ItemFlag_Kind = new ItemFlag_Kind("MATURE");
    public static const COPYRIGHT :ItemFlag_Kind = new ItemFlag_Kind("COPYRIGHT");
    public static const STOLEN :ItemFlag_Kind = new ItemFlag_Kind("STOLEN");
    finishedEnumerating(ItemFlag_Kind);

    /** @private */
    public function ItemFlag_Kind (name :String)
    {
        super(name);
    }

    public static function values () :Array
    {
        return Enum.values(ItemFlag_Kind);
    }

    public static function valueOf (name :String) :ItemFlag_Kind
    {
        return Enum.valueOf(ItemFlag_Kind, name) as ItemFlag_Kind;
    }
}
}
