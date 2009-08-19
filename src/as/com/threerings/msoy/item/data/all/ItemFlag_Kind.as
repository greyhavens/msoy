//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.ByteEnum;
import com.threerings.util.Enum;

public final class ItemFlag_Kind extends ByteEnum
{
    public static const MATURE :ItemFlag_Kind = new ItemFlag_Kind("MATURE", 0);
    public static const COPYRIGHT :ItemFlag_Kind = new ItemFlag_Kind("COPYRIGHT", 1);
    public static const STOLEN :ItemFlag_Kind = new ItemFlag_Kind("STOLEN", 2);
    public static const UNATTRIBUTED :ItemFlag_Kind = new ItemFlag_Kind("UNATTRIBUTED", 3);
    public static const SCAM :ItemFlag_Kind = new ItemFlag_Kind("SCAM", 4);
    public static const BROKEN :ItemFlag_Kind = new ItemFlag_Kind("BROKEN", 5);
    finishedEnumerating(ItemFlag_Kind);

    /** @private */
    public function ItemFlag_Kind (name :String, code :int)
    {
        super(name, code);
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
