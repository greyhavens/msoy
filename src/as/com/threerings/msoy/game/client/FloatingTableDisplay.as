package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.core.ScrollPolicy;

import mx.controls.Button;

import com.threerings.msoy.client.Msgs
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;

import com.threerings.parlor.data.Table;

import com.threerings.util.CommandEvent;

public class FloatingTableDisplay extends FloatingPanel 
    implements TableObserver
{
    public function FloatingTableDisplay (ctx :WorldContext, panel :LobbyPanel, 
        tableDir :TableDirector)
    {
        super(ctx, Msgs.GAME.get("t.table_display"));
        _panel = panel;
        _tableDir = tableDir;
        _tableDir.addTableObserver(this);
        _table = _tableDir.getSeatedTable();

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    public function getRenderer () :TableRenderer
    {
        return _tableRender;
    }

    public function getGameId () :int
    {
        return _panel.getGame().itemId;
    }

    public function shutdown () :void
    {
        _hasBeenShutDown = true;
        close();
        _tableDir.removeTableObserver(this);
    }

    override public function open (modal :Boolean = false, parent :DisplayObject = null,
        avoid :DisplayObject = null) :void
    {
        if (!_hasBeenShutDown) {
            width = 700;
            super.open(modal, parent, avoid);
        }
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // NOOP
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        if (table.tableId == _table.tableId) {
            _table = table;
            _tableRender.data = _table;
        }
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        if (tableId == _table.tableId) {
            shutdown();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _tableRender = new TableRenderer(true);
        _tableRender.ctx = _ctx;
        _tableRender.panel = _panel;
        addChild(_tableRender);
        _tableRender.data = _table;
        
        // make sure the seat grid in TableRenderer takes as much horizontal space as it can
        width = _tableRender.maxUsableWidth > parent.width ? parent.width : 
            _tableRender.maxUsableWidth;
        _tableRender.width = width;
    }

    /** controlled panel to dispatch LobbyController events on */
    protected var _panel :LobbyPanel;

    /** The table we're displaying */
    protected var _table :Table;

    protected var _tableRender :TableRenderer;

    protected var _tableDir :TableDirector;

    protected var _hasBeenShutDown :Boolean = false;
}
}
