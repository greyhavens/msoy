//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on occupants in a game.
 */
public class PlayerInfo extends OccupantInfo
{
    public function PlayerInfo (who :PlayerObject = null)
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

    // from OccupantInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _headShot = (ins.readObject() as MediaDesc);
    }

    // from OccupantInfo
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(_headShot);
    }

    /** The media of the user's headshot (part of their avatar). */
    protected var _headShot :MediaDesc;
}
}
