//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * A base class for sub-items (level packs, etc.).
 */
public class SubItem extends Item
{
    /** The maximum length of game identifiers (used by level and item packs and trophies). */
    public static const MAX_IDENT_LENGTH :int = 32;

    /** The identifier of the suite to which this sub-item belongs. This is either the negated
     * catalogId of the listing for the primary item (if this and the primary item are listed) or
     * the item id of the primary item (if this and the primary item are not listed). */
    public var suiteId :int;

    /** An identifier for this sub-item, used to identify it from code. */
    public var ident :String;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        suiteId = ins.readInt();
        ident = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(suiteId);
        out.writeField(ident);
    }
}
}
