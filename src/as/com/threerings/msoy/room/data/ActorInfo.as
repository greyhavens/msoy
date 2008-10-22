//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.StringBuilder;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.ItemIdent;

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

    /**
     * NOTE: This should only be used in the studio view.
     */
    public function setState (state :String) :void
    {
        _state = state;
    }

    /**
     * Returns true if the server has assigned this actor a static image due to a high room
     * population or low frame rate.
     */
    public function isStatic () :Boolean
    {
        return _static;
    }

    override public function clone () :Object
    {
        var that :ActorInfo = super.clone() as ActorInfo;
        that._media = this._media;
        that._ident = this._ident;
        that._state = this._state;
        that._static = this._static;
        return that;
    }

    // from OccupantInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _media = MediaDesc(ins.readObject());
        _ident = ItemIdent(ins.readObject());
        _state = (ins.readField(String) as String);
        _static = ins.readBoolean();
    }

    /** @inheritDoc */
    // from SimpleStreamableObject
    override protected function toStringBuilder (buf :StringBuilder): void
    {
        super.toStringBuilder(buf);
        buf.append(", media=", _media);
        buf.append(", ident=", _ident);
        buf.append(", state=", _state);
        buf.append(", static=", _static);
    }

    protected var _media :MediaDesc;
    protected var _ident :ItemIdent;
    protected var _state :String;
    protected var _static :Boolean;
}
}
