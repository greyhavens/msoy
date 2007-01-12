//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.ezgame.data.EZGameObject;

/**
 * Represents a lobby for a particular game.
 */
public class WorldGameObject extends EZGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>config</code> field. */
    public static const CONFIG :String = "config";
    
    /** The field name of the <code>memories</code> field. */
    public static const MEMORIES :String = "memories";
    // AUTO-GENERATED: FIELDS END

    /** The game configuration. */
    public var config :WorldGameConfig;
    
    /** Contains the game's memories. */
    public var memories :DSet = new DSet();
    
    override protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        super.readDefaultFields(ins);
        
        config = (ins.readObject() as WorldGameConfig);
        memories = (ins.readObject() as DSet);
    }
}
}
