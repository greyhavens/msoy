//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.data.TableConfig;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.LobbyConfig;
import com.threerings.msoy.game.data.LobbyObject;

public class LobbyController extends PlaceController
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

    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        _mctx = (ctx as WorldContext);
        super.init(ctx, config);
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);
        _tableDir.willEnterPlace(plobj);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);
        _tableDir.didLeavePlace(plobj);
    }

    /**
     * Handles CREATE_TABLE.
     */
    public function handleCreateTable () :void
    {
        _panel.createBtn.enabled = false;
        new TableCreationPanel(_mctx, (_plobj as LobbyObject).game, _panel);
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

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _panel = new LobbyPanel(_mctx, this);
        _tableDir = new TableDirector(_mctx, LobbyObject.TABLES, _panel);
        _tableDir.addSeatednessObserver(_panel);
        return _panel;
    }
    
    /** The provider of free cheese. */
    protected var _mctx :WorldContext;

    /** The panel we're controlling. */
    protected var _panel :LobbyPanel;

    /** The table director. */
    protected var _tableDir :TableDirector;
}
}
