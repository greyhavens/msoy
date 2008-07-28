//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.bureau.data.AgentObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.msoy.avrg.data.SceneInfo;
import com.threerings.io.ObjectInputStream;

public class AVRGameAgentObject extends AgentObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>scenes</code> field. */
    public static const SCENES :String = "scenes";

    /** The field name of the <code>gameOid</code> field. */
    public static const GAME_OID :String = "gameOid";
    // AUTO-GENERATED: FIELDS END

    /** A set of scenes containing (or having recently contained) players of this AVRG. */
    public var scenes :DSet = new DSet();
    SceneInfo; // no-op reference to force link
    
    /** ID of the game object. */
    public var gameOid :int;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        scenes = (ins.readObject() as DSet);
        gameOid = ins.readInt();
    }
}

}
