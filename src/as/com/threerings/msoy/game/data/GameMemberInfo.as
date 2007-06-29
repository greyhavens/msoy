//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Extends MemberInfo with game-specific information.
 */
public class GameMemberInfo extends MemberInfo
{
    public function GameMemberInfo (who :MemberObject = null)
    {
        // only used for unserialization
    }

    /**
     * Returns the headshot URL of this member's avatar.
     */
    public function getHeadshotURL () :String
    {
        return _headShot.getMediaPath();
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _headShot = (ins.readObject() as MediaDesc);
        _humanity = ins.readFloat();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(_headShot);
        out.writeFloat(_humanity);
    }

    /** The media of the user's headshot (part of their avatar). */
    protected var _headShot :MediaDesc;

    /** This member's humanity rating from 0 to 1. */
    protected var _humanity :Number;
}
}
