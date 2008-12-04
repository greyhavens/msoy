//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.StringBuilder;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.GameSummary;

/**
 * Contains published information about a member in a scene.
 */
public class MemberInfo extends ActorInfo
{
    /**
     * Get the member id for this user, or 0 if they're a guest.
     */
    public function getMemberId () :int
    {
        return (username as MemberName).getMemberId();
    }

    /**
     * Return true if we represent a guest.
     */
    public function isGuest () :Boolean
    {
        return MemberName.isGuest(getMemberId());
    }

    /**
     * Returns information on a game this user is currently lobbying or playing.
     */
    public function getGameSummary () :GameSummary
    {
        return _game;
    }

    /**
     * Return the scale that should be used for the media.
     */
    public function getScale () :Number
    {
        return _scale;
    }

    /**
     * Update the scale. This method currently only exists on the actionscript side.  We update the
     * scale immediately when someone is futzing with the scale in the avatar viewer.
     */
    public function setScale (scale :Number) :void
    {
        _scale = scale;
    }

    /**
     * Tests if this member is able to manage the room.
     * Note that this is not a definitive check, but rather one that can be used by clients
     * to check other occupants. The value is computed at the time the occupant enters the
     * room, and is not recomputed even if the room ownership changes. The server should
     * continue to do definitive checks where it matters.
     */
    public function isManager () :Boolean
    {
        return (_flags & MANAGER) != 0;
    }

    // from ActorInfo
    override public function clone () :Object
    {
        var that :MemberInfo = super.clone() as MemberInfo;
        that._scale = this._scale;
        that._game = this._game;
        return that;
    }

    // from ActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _scale = ins.readFloat();
        _game = GameSummary(ins.readObject());
    }

    /** @inheritDoc */
    // from SimpleStreamableObject
    override protected function toStringBuilder (buf :StringBuilder): void
    {
        super.toStringBuilder(buf);
        buf.append(", scale=", _scale);
        buf.append(", game=", _game);
    }

    protected var _scale :Number;
    protected var _game :GameSummary;
}
}
