//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Maps a Party to its leader in a PartyPlaceObject.
 */
public class PartyLeader
    implements DSet_Entry
{
    /** The id of the party. */
    public var partyId :int;

    /** The memberId of the leader. */
    public var leaderId :int;

    // from DSet.Entry
    public function getKey () :Object
    {
        return partyId;
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }

    public function readObject (ins :ObjectInputStream) :void
    {
        partyId = ins.readInt();
        leaderId = ins.readInt();
    }
}
}
