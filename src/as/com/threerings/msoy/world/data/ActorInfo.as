//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains published information about an actor in a scene (members and pets).
 */
public class ActorInfo extends OccupantInfo
{
    /**
     * Returns the media that is used to display this actor.
     */
    public function getMedia () :MediaDesc
    {
        return _media;
    }

    /**
     * Returns the item identifier that is used to identify this actor.
     */
    public function getItemIdent () :ItemIdent
    {
        return _ident;
    }

    /**
     * Return the current state of the actor, which may be null.
     */
    public function getState () :String
    {
        return _state;
    }

    // from OccupantInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _media = (ins.readObject() as MediaDesc);
        _ident = (ins.readObject() as ItemIdent);
        _state = (ins.readField(String) as String);
    }

    protected var _media :MediaDesc;
    protected var _ident :ItemIdent;
    protected var _state :String;
}
}
