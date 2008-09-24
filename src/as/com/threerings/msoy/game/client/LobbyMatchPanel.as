//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.Text;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;

/**
 * Displays the current tables list and related bits.
 */
public class LobbyMatchPanel extends VBox
    implements TableObserver
{
    public function LobbyMatchPanel (gctx :GameContext, ctrl :LobbyController, lobj :LobbyObject)
    {
        _gctx = gctx;
        _ctrl = ctrl;
        _lobj = lobj;

        styleName = "lobbyMatchPanel";
        percentWidth = 100;
        percentHeight = 100;
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // if it's a running unwatchable seated game, no need to display it
        var inPlayUnwatchable :Boolean = (table.config.getMatchType() != GameConfig.PARTY) &&
            table.inPlay() && (_lobj.gameDef.match as MsoyMatchConfig).unwatchable;
        // if it's a private table, we also don't display it (TODO: what does private table really
        // mean, how does anyone ever join a private table?)
        if (inPlayUnwatchable || table.tconfig.privateTable) {
            return;
        }

        // add the table at the bottom of the list
        var list :VBox = (table.gameOid > 0) ? _runningList : _pendingList;
        var panel :TableSummaryPanel = new TableSummaryPanel(_gctx, _lobj);
        list.addChild(panel);
        panel.update(table);
        updateTableState();
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        var panel :TableSummaryPanel = getTablePanel(table.tableId);

        // if we have no ui for it, no problem, stop here
        if (panel == null) {
            return;
        }

        // if the table switched from pending to running, move it
        if (table.gameOid > 0 && panel.parent == _pendingList) {
            _pendingList.removeChild(panel);
            // if it's a running unwatchable seated game, no need to display it
            if (table.config.getMatchType() == GameConfig.PARTY || 
                !(_lobj.gameDef.match as MsoyMatchConfig).unwatchable) {
                _runningList.addChild(panel);
            }
            updateTableState();
        }

        // and update it
        panel.update(table);
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        var panel :TableSummaryPanel = getTablePanel(tableId);
        if (panel != null) {
            panel.parent.removeChild(panel);
            updateTableState();
        }
    }

    override public function parentChanged (parent :DisplayObjectContainer) :void
    {
        super.parentChanged(parent);

        if (parent == null) {
            _ctrl.tableDir.removeTableObserver(this);
        } else {
            _ctrl.tableDir.addTableObserver(this);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var createBtn :CommandButton = new CommandButton(Msgs.GAME.get("b.create_multi"),
            _ctrl.panel.setMode, LobbyController.MODE_CREATE);
        addChild(createBtn);

        // determine our informational messages
        var noPendersMsg :String, pendersHeader :String, runningHeader :String;
        if (GameConfig.SEATED_GAME == _lobj.gameDef.match.getMatchType()) {
            noPendersMsg = Msgs.GAME.get("m.no_penders_seated");
            pendersHeader = Msgs.GAME.get("l.penders_header_seated");
            if ((_lobj.gameDef.match as MsoyMatchConfig).unwatchable) {
                runningHeader = Msgs.GAME.get("l.running_header_seated_nowatch");
            } else {
                runningHeader = Msgs.GAME.get("l.running_header_seated");
            }

        } else {
            noPendersMsg = Msgs.GAME.get("m.no_penders_party");
            pendersHeader = ""; // this is never used party games start immediately
            runningHeader = Msgs.GAME.get("l.running_header_party");
        }

        _noTablesLabel = new Text();
        _noTablesLabel.styleName = "tableMessage";
        _noTablesLabel.text = noPendersMsg;
        _noTablesLabel.percentWidth = 100;
        addChild(_noTablesLabel);

        addChild(_pendingList = new VBox());
        _pendingList.styleName = "pendingTableList";
        _pendingList.percentWidth = 100;
        var header :HBox = new HBox();
        header.percentWidth = 100;
        header.styleName = "tableHeader";
        header.addChild(FlexUtil.createLabel(pendersHeader));
        _pendingList.addChild(header);

        addChild(_runningList = new VBox());
        _runningList.styleName = "runningTableList";
        _runningList.percentWidth = 100;
        header = new HBox();
        header.percentWidth = 100;
        header.styleName = "tableHeader";
        header.addChild(FlexUtil.createLabel(runningHeader));
        _runningList.addChild(header);

        // update our existing tables
        for each (var table :Table in _lobj.tables.toArray()) {
            tableAdded(table);
        }
        updateTableState();
    }

    protected function updateTableState () :void
    {
        var havePending :Boolean = (_pendingList.numChildren > 1);
        var haveRunning :Boolean = (_runningList.numChildren > 1);
        FlexUtil.setVisible(_pendingList, havePending);
        FlexUtil.setVisible(_runningList, haveRunning);
        FlexUtil.setVisible(_noTablesLabel, !(havePending || haveRunning));
    }

    protected function getTablePanel (tableId :int) :TableSummaryPanel
    {
        for (var ii :int = 0; ii < _pendingList.numChildren; ii++) {
            var child :TableSummaryPanel = (_pendingList.getChildAt(ii) as TableSummaryPanel);
            if (child != null && child.tableId == tableId) {
                return child;
            }
        }
        for (ii = 0; ii < _runningList.numChildren; ii++) {
            child = (_runningList.getChildAt(ii) as TableSummaryPanel);
            if (child != null && child.tableId == tableId) {
                return child;
            }
        }
        return null;
    }

    protected var _gctx :GameContext;
    protected var _ctrl :LobbyController;
    protected var _lobj :LobbyObject;

    protected var _noTablesLabel :Text;
    protected var _pendingList :VBox;
    protected var _runningList :VBox;
}
}
