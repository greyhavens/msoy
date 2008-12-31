//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Integer;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains information on the current location of a member.
 */
public class MemberLocation
    implements Streamable, DSet_Entry
{
    /** The id of the member represented by this location. */
    public var memberId :int;

    /** The id of the scene occupied by this member or 0. */
    public var sceneId :int;

    /** The id of the game or game lobby occupied by this member. */
    public var gameId :int;

    /** Whether or not this is an AVRGame. */
    public var avrGame :Boolean;

    // from DSet_Entry
    public function getKey () :Object
    {
        return memberId;
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(new Integer(memberId));
        out.writeInt(sceneId);
        out.writeInt(gameId);
        out.writeBoolean(avrGame);
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        memberId = (ins.readField(Integer) as Integer).value;
        sceneId = ins.readInt();
        gameId = ins.readInt();
        avrGame = ins.readBoolean();
    }
}
}
