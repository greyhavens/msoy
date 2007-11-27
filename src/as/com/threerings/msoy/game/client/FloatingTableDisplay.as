//
// $Id$

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
    public function FloatingTableDisplay (
        ctx :GameContext, panel :LobbyPanel, tableDir :TableDirector, gameName :String)
    {
        super(ctx.getWorldContext(), Msgs.GAME.get("t.table_display") + gameName);

        _gctx = ctx;
        _panel = panel;
        _tableDir = tableDir;
        _tableDir.addTableObserver(this);
        _table = _tableDir.getSeatedTable();

        styleName = "floatingTableDisplay";
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

    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
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
            if (table.gameOid > 0) {
                shutdown();
            } else {
                _table = table;
                _tableRender.data = _table;
            }
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

        var row :HBox = new HBox();
        row.styleName = "floatingTableRow";
        row.percentWidth = 100;
        row.percentHeight = 100;
        addChild(row);

        var btnBox :VBox = new VBox();
        btnBox.styleName = "backToLobbyBtnBox";
        row.addChild(btnBox);

        var restoreBtn :CommandButton = new CommandButton();
        restoreBtn.setCallback(_panel.controller.restoreLobbyUI);
        restoreBtn.styleName = "backToLobbyBtn";
        btnBox.addChild(restoreBtn);

        var padding :VBox = new VBox();
        padding.setStyle("backgroundColor", 0xE0E7EE);
        padding.width = 2;
        padding.percentHeight = 100;
        row.addChild(padding);

        _tableRender = new TableRenderer(true);
        _tableRender.gctx = _gctx;
        _tableRender.panel = _panel;
        row.addChild(_tableRender);
        _tableRender.data = _table;
    }

    protected var _gctx :GameContext;
    protected var _panel :LobbyPanel;
    protected var _table :Table;
    protected var _tableRender :TableRenderer;
    protected var _tableDir :TableDirector;
    protected var _hasBeenShutDown :Boolean = false;
}
}
