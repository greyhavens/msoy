//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * Contains information on the current location of a member.
 */
public class MemberLocation
    implements Streamable
{
    /** The id of the member represented by this location. */
    public var memberId :int;

    /** The id of the scene occupied by this member or 0. */
    public var sceneId :int;

    /** The id of the game or game lobby occupied by this member or 0. */
    public var gameId :int;

    /** Whether or not the member's game is AVR. */
    public var avrGame :Boolean;

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(memberId);
        out.writeInt(sceneId);
        out.writeInt(gameId);
        out.writeBoolean(avrGame);
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        memberId = ins.readInt();
        sceneId = ins.readInt();
        gameId = ins.readInt();
        avrGame = ins.readBoolean();
    }
}
}
