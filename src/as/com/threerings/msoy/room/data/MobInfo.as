//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Represents an AVRG MOB.
 */
public class MobInfo extends OccupantInfo
{
    /**
     * Returns the gameId of the AVRG that spawned this MOB and is responsible for it on the
     * client side.
     */
    public function getGameId () :int
    {
        return _gameId;
    }

    /**
     * Returns the string identifier for this MOB. This is provided by the AVRG and interpreted by
     * the AVRG, we don't parse it.
     */
    public function getIdent () :String
    {
        return _ident;
    }

    // from ActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _gameId = ins.readInt();
        _ident = (ins.readField(String) as String);
    }

    protected var _gameId :int;

    protected var _ident :String;
}
}
