//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.whirled.game.data.TableMatchConfig;

/**
 * Class to encapsulate extended properties to table matches for Whirled.
 */
public class MsoyMatchConfig extends TableMatchConfig
{
    /** The creator configured matchmaking type. */
    public var type :int;

    /** Whether this game is watchable or not. Defaults to not. */
    public var unwatchable :Boolean = false;

    public function MsoyMatchConfig ()
    {
    }

    // from MatchConfig
    override public function getMatchType () :int
    {
        return type;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        type = ins.readInt();
        unwatchable = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(type);
        out.writeBoolean(unwatchable);
    }
}
}
