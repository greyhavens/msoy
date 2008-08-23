//
// $Id$

package com.threerings.msoy.game.data {

import flash.errors.IllegalOperationError;

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableLobbyObject;
import com.threerings.parlor.data.TableMarshaller;

import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.item.data.all.Game;

/**
 * Represents a lobby for a particular game.
 */
public class LobbyObject extends DObject implements TableLobbyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>game</code> field. */
    public static const GAME :String = "game";

    /** The field name of the <code>tables</code> field. */
    public static const TABLES :String = "tables";

    /** The field name of the <code>groupId</code> field. */
    public static const GROUP_ID :String = "groupId";
    // AUTO-GENERATED: FIELDS END

    /** The game we're matchmaking for. */
    public var game :Game;

    /** The parsed configuration info for this game. */
    public var gameDef :GameDefinition;

    /** The tables. */
    public var tables :DSet = new DSet();

    /** Used to communicate with table manager. */
    public var tableService :TableMarshaller;
    
    /** The group to load up behind the lobby if not already in a room. */
    public var groupId :int;

    // from TableLobbyObject
    public function getTables () :DSet
    {
        return tables;
    }

    // from TableLobbyObject
    public function addToTables (table :Table) :void
    {
        throw new IllegalOperationError(); // not applicable on client
    }

    // from TableLobbyObject
    public function removeFromTables (key :Object) :void
    {
        throw new IllegalOperationError(); // not applicable on client
    }

    // from TableLobbyObject
    public function updateTables (table :Table) :void
    {
        throw new IllegalOperationError(); // not applicable on client
    }

    // from TableLobbyObject
    public function getTableService () :TableMarshaller
    {
        return tableService;
    }

    // from TableLobbyObject
    public function setTableService (tableService :TableMarshaller) :void
    {
        throw new IllegalOperationError(); // not applicable on client
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        game = (ins.readObject() as Game);
        gameDef = (ins.readObject() as GameDefinition);
        tables = (ins.readObject() as DSet);
        tableService = (ins.readObject() as TableMarshaller);
        groupId = (ins.readInt());
    }
}
}
