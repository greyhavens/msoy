//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.data.ActorInfo;

/**
 * Contains extra information for an occupant when they are in the virtual world.
 */
public class WorldActorInfo extends ActorInfo
    implements WorldOccupantInfo
{
    /** The current state of the actor's avatar. */
    public var state :String;

    // from interface WorldOccupantInfo
    public function getMedia () :MediaDesc
    {
        return _media;
    }

    // from interface WorldOccupantInfo
    public function getState () :String
    {
        return state;
    }

    // from ActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        state = (ins.readField(String) as String);
        _media = (ins.readObject() as MediaDesc);
    }

    /** The media that represents this occupant. */
    protected var _media :MediaDesc;
}
}
