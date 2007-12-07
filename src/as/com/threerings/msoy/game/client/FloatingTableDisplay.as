//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.flex.CommandButton;
import com.threerings.util.CommandEvent;

import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;

import com.threerings.msoy.client.Msgs

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;

public class FloatingTableDisplay extends FloatingPanel
    implements TableObserver
{
    public function FloatingTableDisplay (mctx :MsoyContext, gctx :GameContext, panel :LobbyPanel,
                                          tableDir :TableDirector, gameName :String)
    {
        super(mctx, Msgs.GAME.get("t.table_display") + gameName);

        _gctx = gctx;
        _panel = panel;
        _tableDir = tableDir;
        _tableDir.addTableObserver(this);
        _table = _tableDir.getSeatedTable();

        styleName = "floatingTableDisplay";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    public function getPanel () :TablePanel
    {
        return _tablePanel;
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
                _tablePanel.update(_table, true);
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

        row.addChild(_tablePanel = new TablePanel(_gctx, _panel, _table, true));
    }

    protected var _gctx :GameContext;
    protected var _panel :LobbyPanel;
    protected var _table :Table;
    protected var _tablePanel :TablePanel;
    protected var _tableDir :TableDirector;
    protected var _hasBeenShutDown :Boolean = false;
}
}
