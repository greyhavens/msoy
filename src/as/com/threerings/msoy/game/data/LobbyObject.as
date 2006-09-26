//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableLobbyObject;

import com.threerings.msoy.item.web.Game;

/**
 * Represents a lobby for a particular game.
 */
public class LobbyObject extends PlaceObject
//    implements TableLobbyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tables</code> field. */
    public static const TABLES :String = "tables";
    // AUTO-GENERATED: FIELDS END

    /** The tables. */
    public var tables :DSet = new DSet();

//    // from TableLobbyObject
//    public function getTables () :DSet
//    {
//        return tables;
//    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        tables = (ins.readObject() as DSet);
    }
}
}
