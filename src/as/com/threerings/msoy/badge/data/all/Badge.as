package com.threerings.msoy.badge.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet_Entry;

public /* abstract */ class Badge extends SimpleStreamableObject
    implements DSet_Entry
{
    /** The code that uniquely identifies this badge type. */
    public var badgeCode :int;

    /** The level that pertains to this Badge object. */
    public var level :int;

    /** The level units that can be used in a translation message for the description of this
     * badge level. */
    public var levelUnits :String;

    /** The reward the player will receive for completing the current level. */
    public var coinValue :int;

    // from DSet_Entry
    public function getKey () :Object
    {
        return badgeCode;
    }

    /**
     * Returns the name of this badge.
     */
    public function get nameProp () :String
    {
        return "m.badge_name_" + uint(badgeCode).toString(16);
    }

    /**
     * Returns the public access image URL for this badge.
     */
    public function get imageUrl () :String
    {
        throw new Error("abstract");
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        badgeCode = ins.readInt();
        level = ins.readInt();
        levelUnits = ins.readField(String) as String;
        coinValue = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(badgeCode);
        out.writeInt(level);
        out.writeField(levelUnits);
        out.writeInt(coinValue);
    }

    protected static const BADGE_IMAGE_DIR :String = "badge/";
    protected static const BADGE_IMAGE_TYPE :String = ".png";
}
}
