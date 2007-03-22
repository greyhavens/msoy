//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.data.TableConfig;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.LobbyObject;

import com.threerings.util.Controller;

public class LobbyController extends Controller implements Subscriber
{
    /** A command to create a new table. */
    public static const CREATE_TABLE :String = "CreateTable";

    /** A command to submit a configured table configuration for creation. */
    public static const SUBMIT_TABLE :String = "SubmitTable";

    /** A command to start a table early (when everyone's not yet sitting) */
    public static const START_TABLE :String = "StartTable";

    /** A command to sit in a place in a table. */
    public static const SIT :String = "Sit";

    /** A command to leave a table. */
    public static const LEAVE :String = "Leave";

    /** A command to leave the lobby. */
    public static const LEAVE_LOBBY :String = "LeaveLobby";

    public function LobbyController (mctx :WorldContext, oid :int) 
    {
        _mctx = mctx;

        _subscriber = new SafeSubscriber(oid, this)
        _subscriber.subscribe(_mctx.getDObjectManager());
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void 
    {
        _lobj = obj as LobbyObject;
        _panel = new LobbyPanel(_mctx, this, _lobj);
        _panel.addEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        setControlledPanel(_panel);

        _tableDir = new TableDirector(_mctx, LobbyObject.TABLES, _panel);
        _tableDir.setTableObject(obj);
        _tableDir.addSeatednessObserver(_panel);

        _mctx.getTopPanel().setSidePanel(_panel);
    }

    /**
     * Event handler for Event.REMOVED_FORM_STAGE
     */
    public function handleRemovedFromStage (evt :Event) :void
    {
        _panel.removeEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        _subscriber.unsubscribe(_mctx.getDObjectManager());
        if (_tableDir.isSeated()) {
            _tableDir.leaveTable(_tableDir.getSeatedTable().tableId);
        }
    }

    // from Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void 
    {
        Log.getLog(this).warning("request for the LobbyObject failed: ", cause);
    }

    /**
     * Handles CREATE_TABLE.
     */
    public function handleCreateTable () :void
    {
        _panel.createBtn.enabled = false;
        new TableCreationPanel(_mctx, _lobj.game, _panel);
    }

    /**
     * Handles SUBMIT_TABLE.
     */
    public function handleSubmitTable (args :Array) :void
    {
        _tableDir.createTable(args[0] as TableConfig, args[1] as GameConfig);
    }

    /**
     * Handles SIT.
     */
    public function handleSit (args :Array) :void
    {
        _tableDir.joinTable(args[0] as int /*tableId*/, args[1] as int /*pos*/);
    }

    /**
     * Handles LEAVE.
     */
    public function handleLeave (tableId :int) :void
    {
        _tableDir.leaveTable(tableId);
    }

    /**
     * Handles START_TABLE.
     */
    public function handleStartTable (tableId :int) :void
    {
        _tableDir.startTableNow(tableId);
    }

    /**
     * Handles LEAVE_LOBBY.
     */
    public function handleLeaveLobby () :void
    {
        _mctx.getTopPanel().clearSidePanel(_panel);
    }

    /** The provider of free cheese. */
    protected var _mctx :WorldContext;

    /** The panel we're controlling. */
    protected var _panel :LobbyPanel;

    /** The table director. */
    protected var _tableDir :TableDirector;

    /** Our distributed LobbyObject */
    protected var _lobj :LobbyObject;

    protected var _subscriber :SafeSubscriber;
}
}
