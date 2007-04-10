package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.msoy.client.Msgs
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;

import com.threerings.parlor.data.Table;

import com.threerings.flex.CommandButton;
import com.threerings.util.CommandEvent;

public class FloatingTableDisplay extends FloatingPanel 
    implements TableObserver
{
    public function FloatingTableDisplay (ctx :WorldContext, panel :LobbyPanel, 
        tableDir :TableDirector, gameName :String)
    {
        super(ctx, Msgs.GAME.get("t.table_display") + gameName);
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

        styleName = "floatingTableDisplay";

        var row :HBox = new HBox();
        row.styleName = "floatingTableRow";
        row.percentWidth = 100;
        row.percentHeight = 100;
        addChild(row);
        var btnBox :VBox = new VBox();
        btnBox.styleName = "backToLobbyBtnBox";
        row.addChild(btnBox);
        var joinLobbyBtn :CommandButton = new CommandButton();
        joinLobbyBtn.setFunction(function () :void {
            CommandEvent.dispatch(_tableRender, LobbyController.JOIN_LOBBY);
        });
        joinLobbyBtn.styleName = "backToLobbyBtn";
        btnBox.addChild(joinLobbyBtn);
        var padding :VBox = new VBox();
        padding.setStyle("backgroundColor", 0xE0E7EE);
        padding.width = 2;
        padding.percentHeight = 100;
        row.addChild(padding);
        _tableRender = new TableRenderer(true);
        _tableRender.ctx = _ctx;
        _tableRender.panel = _panel;
        row.addChild(_tableRender);
        _tableRender.data = _table;
        
        // make sure the seat grid in TableRenderer takes as much horizontal space as it can
        var extraSpace :int = 20;
        _tableRender.width = _tableRender.maxUsableWidth > (parent.width - extraSpace) ? 
            parent.width - extraSpace : _tableRender.maxUsableWidth;
        width = _tableRender.width + extraSpace;
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
