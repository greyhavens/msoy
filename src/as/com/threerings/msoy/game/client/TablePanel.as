//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.controls.Label;
import mx.controls.Text;

import com.threerings.util.Log;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.flex.CommandButton;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.ezgame.data.EZGameConfig;
import com.threerings.ezgame.data.GameDefinition;
import com.threerings.ezgame.data.Parameter;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.ui.SimpleGrid;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTable;

public class TablePanel extends VBox
{
    public var tableId :int;

    public function TablePanel (gctx :GameContext, panel :LobbyPanel, table :MsoyTable,
                                   popup :Boolean = false)
    {
        styleName = popup ? "floatingTablePanel" : "listTablePanel";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        percentWidth = 100;
        tableId = table.tableId;

        _gctx = gctx;
        _game = panel.getGame();
        _gameDef = panel.getGameDefinition();

        // create our seats grid
        addChild(_seatsGrid = new SimpleGrid(9));
        _seatsGrid.verticalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.horizontalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.styleName = "seatsGrid";
        for (var ii :int = 0; ii < table.occupants.length; ii++) {
            var seat :SeatPanel = new SeatPanel();
            _seatsGrid.addCell(seat);
        }
        _seatsGrid.setStyle("horizontalGap",
                            10 * (popup ? 1 : Math.max(1, 8-table.occupants.length)));

        // create a box to hold configuration options if we're not a popup
        if (!popup) {
            _labelsBox = new HBox();
            _labelsBox.verticalScrollPolicy = ScrollPolicy.OFF;
            _labelsBox.horizontalScrollPolicy = ScrollPolicy.OFF;
            addChild(_labelsBox);
        }

        // if we are the creator, add a button for starting the game now
        if (table.occupants.length > 0 &&
            gctx.getPlayerObject().getVisibleName().equals(table.occupants[0]) &&
            (table.tconfig.desiredPlayerCount > table.tconfig.minimumPlayerCount)) {
            _startBtn = new CommandButton(LobbyController.START_TABLE, table.tableId);
            _startBtn.label = Msgs.GAME.get("b.start_now");
            _seatsGrid.addCell(_startBtn);
        }

        // maybe add a button for entering the game
        if (table.gameOid != -1) {
            var key :String = null;
            switch (table.config.getMatchType()) {
            case GameConfig.PARTY:
                if (!table.tconfig.privateTable) {
                    key = "b.join";
                }
                break;

            default:
                if (!(_gameDef.match as MsoyMatchConfig).unwatchable &&
                    !table.tconfig.privateTable) {
                    key = "b.watch";
                }
                break;
            }

            if (key != null) {
                var btn :CommandButton = new CommandButton(
                    MsoyController.GO_GAME, [ _game.gameId, table.gameOid ]);
                btn.label = Msgs.GAME.get(key);
                _seatsGrid.addCell(btn);
            }
        }

        // finally update our state
        update(table, panel.isSeated());
    }

    public function update (table :MsoyTable, isSeated :Boolean) :void
    {
        // update our background color based on whether or not we're running
        setStyle("backgroundColor", (table.gameOid > 0) ? 0xEEEEEE : 0xFFFFFF);
        if (_startBtn != null) {
            _startBtn.enabled = table.mayBeStarted();
        }
        for (var ii :int = 0; ii < table.occupants.length; ii++) {
            (_seatsGrid.getCellAt(ii) as SeatPanel).update(_gctx, table, ii, isSeated);
        }

        // update our configuration labels if we have them
        if (_labelsBox == null) {
            return;
        }
        while (_labelsBox.numChildren > 0) {
            _labelsBox.removeChild(_labelsBox.getChildAt(0));
        }

        // if the game is in progress, report the number of people in the room
        if (table.gameOid != -1) {
            var wc :String = String(table.watcherCount);
            _labelsBox.addChild(makeConfigLabel(Msgs.GAME.get("l.people"), wc));
        }

        if (table.config is EZGameConfig) {
            var params :Array = (table.config as EZGameConfig).getGameDefinition().params;
            if (params != null) {
                var ezconfig :EZGameConfig = (table.config as EZGameConfig);
                for each (var param :Parameter in params) {
                        var name :String = StringUtil.isBlank(param.name) ?
                                 param.ident : param.name;
                        var value :String = String(ezconfig.params.get(param.ident));
                        _labelsBox.addChild(makeConfigLabel(name, value, param.tip));
                    }
            }
        }
    }

    protected function makeConfigLabel (name :String, value :String, tip :String = "") :UIComponent
    {
        var label :Label = MsoyUI.createLabel(name + ": " + value, "tableStatusLabel");
        if (tip != "") {
            label.toolTip = tip;
        }
        return label;
    }

    protected var _gctx :GameContext;
    protected var _watcherCount :int;
    protected var _labelsBox :HBox;
    protected var _seatsGrid :SimpleGrid;
    protected var _startBtn :CommandButton;

    protected var _game :Game;
    protected var _gameDef :GameDefinition;
}
}

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.data.MsoyTable;

class SeatPanel extends VBox
{
    public function SeatPanel () :void
    {
        styleName = "seatPanel";
    }

    public function update (ctx :GameContext, table :MsoyTable, index :int, areSeated :Boolean) :void
    {
        _ctx = ctx;
        _table = table;
        _index = index;

        var occupant :MemberName = (_table.occupants[_index] as MemberName);
        if (occupant == null) {
            prepareJoinButton();
            _joinBtn.enabled = (table.gameOid <= 0) && !areSeated;
            return;
        }

        prepareOccupant();
        _headShot.setMediaDesc(_table.headShots[_index] as MediaDesc);
        _name.text = occupant.toString();
        if (occupant.equals(_ctx.getPlayerObject().memberName)) {
            _leaveBtn.setCommand(LobbyController.LEAVE_TABLE, _table.tableId);
            _leaveBtn.visible = (_leaveBtn.includeInLayout = true);
        } else {
            _leaveBtn.visible = (_leaveBtn.includeInLayout = false);
        }

        // TODO: add support for booting players from tables to the TableService, make it 
        // optional on TableManager creation, and support it here in the form of the closebox
    }

    protected function prepareOccupant () :void
    {
        if (_name == null || _name.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }

            var hbox :HBox = new HBox();
            hbox.setStyle("horizontalGap", 5);
            hbox.addChild(new MediaWrapper(_headShot = new ScalingMediaContainer(40, 30), 40, 30));
            hbox.addChild(_leaveBtn = new CommandButton());
            _leaveBtn.styleName = "closeButton";
            addChild(hbox);
            addChild(_name = MsoyUI.createLabel("", "nameLabel"));
        } 
    }

    protected function prepareJoinButton () :void
    {
        if (_joinBtn == null || _joinBtn.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }
            if (_joinBtn == null) {
                _joinBtn = new CommandButton(LobbyController.JOIN_TABLE, [ _table.tableId, _index ]);
                _joinBtn.label = Msgs.GAME.get("b.join");
            }
            addChild(_joinBtn);
        }
    }

    protected var _ctx :GameContext;
    protected var _table :MsoyTable;
    protected var _index :int;

    protected var _joinBtn :CommandButton;
    protected var _headShot :ScalingMediaContainer;
    protected var _name :Label;
    protected var _leaveBtn :CommandButton;
}
