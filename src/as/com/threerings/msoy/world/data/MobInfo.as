//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Represents an AVRG MOB.
 */
public class MobInfo extends OccupantInfo
{
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
        _ident = (ins.readField(String) as String);
    }

    protected var _ident :String;
}
}
