package com.threerings.msoy.badge.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Long;

/**
 * Contains information on a badge earned by the player.
 */
public class EarnedBadge extends Badge
{
    /** When this badge was earned. */
    public var whenEarned :Long;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        whenEarned = (ins.readField(Long) as Long);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(whenEarned);
    }
}

}
