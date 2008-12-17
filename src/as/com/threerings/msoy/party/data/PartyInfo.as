//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

/**
 * Summarized party info that is both published to the node objects and returned
 * to users as part of the party board.
 *
 * NOTE: please be careful about what fields you add. If fields are added that are needed by
 * one usage but not the other, we may need to consider having two different objects...
 */
public class PartyInfo extends SimpleStreamableObject
{
    /** The unique party id. */
    public var id :int;

    /** The name of this party. */
    public var name :String;

    /** The memberId of the leader of the party. */
    public var leaderId :int;

    /** The group sponsoring this party. */
    public var groupId :int;

    /** The status line indicating what this party is doing. */
    public var status :String;

    /** The current population of this party. */
    public var population :int;

    /** The current recruitment status of this party. */
    public var recruitment :int;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        id = ins.readInt();
        name = ins.readField(String) as String;
        leaderId = ins.readInt();
        groupId = ins.readInt();
        status = ins.readField(String) as String;
        population = ins.readInt();
        recruitment = ins.readByte();
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }
}
}
