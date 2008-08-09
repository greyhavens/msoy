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
import com.threerings.flex.FlexUtil;

import com.threerings.parlor.data.Parameter;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;

import com.whirled.game.data.WhirledGameConfig;
import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.ui.SimpleGrid;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTableConfig;

public class TablePanel extends VBox
{
    public var tableId :int;

    public function TablePanel (gctx :GameContext, panel :LobbyPanel, table :Table)
    {
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
        for (var ii :int = 0; ii < table.players.length; ii++) {
            var seat :SeatPanel = new SeatPanel();
            _seatsGrid.addCell(seat);
        }
        _seatsGrid.setStyle("horizontalGap", 10 * Math.max(1, 8-table.players.length));

        var box :HBox = new HBox();
        box.percentWidth = 100;
        addChild(box);

        _title = new Text();
        _title.styleName = "tableTitleLabel";
        box.addChild(_title);

        // create a place to hold configuration info
        _info = new Text();
        _info.styleName = "tableStatusLabel";
        _info.percentWidth = 100;
        box.addChild(_info);

        // if we are the creator, add a button for starting the game now
        if (table.players.length > 0 &&
            gctx.getPlayerObject().getVisibleName().equals(table.players[0]) &&
            (table.tconfig.desiredPlayerCount > table.tconfig.minimumPlayerCount)) {
            _startBtn = new CommandButton(
                Msgs.GAME.get("b.start_now"), LobbyController.START_TABLE, table.tableId);
            _seatsGrid.addCell(_startBtn);
        }

        // finally update our state
        update(table, panel.isSeated());
    }

    public function update (table :Table, isSeated :Boolean) :void
    {
        // update our background color based on whether or not we're running
        styleName = (table.gameOid > 0) ? "runningTablePanel" : "tablePanel";
        if (_startBtn != null) {
            _startBtn.enabled = table.mayBeStarted();
        }
        for (var ii :int = 0; ii < table.players.length; ii++) {
            (_seatsGrid.getCellAt(ii) as SeatPanel).update(_gctx, table, ii, isSeated);
        }

        // if the game is in progress
        if (table.gameOid != -1) {
            // maybe add a button for entering the game if we haven't already
            if (_seatsGrid.cellCount == 0 ||
                (_seatsGrid.getCellAt(_seatsGrid.cellCount-1) is SeatPanel)) {
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
                    var btn :CommandButton = new CommandButton(Msgs.GAME.get(key),
                        MsoyController.GO_GAME, [ _game.gameId, table.gameOid ]);
                    _seatsGrid.addCell(btn);
                }
            }
        }

        // display whether this game is rated
        var info :String = Msgs.GAME.get(table.config.rated ? "l.is_rated" : "l.not_rated");

        // display the non-players in the room (or everyone for party games)
        if (table.watchers != null && table.watchers.length > 0) {
            info = info + ", " + Msgs.GAME.get("l.people") + ": " + table.watchers.join(", ");
        }

        // and display any custom table configuration
        if (table.config is WhirledGameConfig) {
            var gconfig :WhirledGameConfig = (table.config as WhirledGameConfig);
            var params :Array = gconfig.getGameDefinition().params;
            if (params != null) {
                for each (var param :Parameter in params) {
                    var name :String = StringUtil.isBlank(param.name) ? param.ident : param.name;
                    var value :String = String(gconfig.params.get(param.ident));
                    info = info + ", " + name + ": "+ value;
                }
            }
        }

        _title.text = (table.tconfig as MsoyTableConfig).title;
        _info.text = info;
    }

    protected var _gctx :GameContext;
    protected var _watcherCount :int;
    protected var _title :Text;
    protected var _info :Text;
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
import com.threerings.flex.FlexUtil;

import com.threerings.parlor.data.Table;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.client.LobbyController;

class SeatPanel extends VBox
{
    public function SeatPanel () :void
    {
        styleName = "seatPanel";
    }

    public function update (ctx :GameContext, table :Table, index :int, areSeated :Boolean) :void
    {
        _ctx = ctx;
        _table = table;
        _index = index;

        var player :VizMemberName = (_table.players[_index] as VizMemberName);
        if (player == null) {
            prepareJoinButton();
            _joinBtn.enabled = (table.gameOid <= 0) && !areSeated;
            return;
        }

        preparePlayer();
        _headShot.setMediaDesc(player.getPhoto());
        _name.text = player.toString();
        var canLeave :Boolean = false;
        if (player.equals(_ctx.getPlayerObject().memberName)) {
            _leaveBtn.setCommand(LobbyController.LEAVE_TABLE, _table.tableId);
            canLeave = true;
        } else if (_ctx.getPlayerObject().memberName.equals(table.players[0])) {
            _leaveBtn.setCommand(LobbyController.BOOT_PLAYER, [ _table.tableId, _index ]);
            canLeave = true;
        }
        FlexUtil.setVisible(_leaveBtn, canLeave);
    }

    protected function preparePlayer () :void
    {
        if (_name == null || _name.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }

            var hbox :HBox = new HBox();
            hbox.setStyle("horizontalGap", 5);
            hbox.addChild(_headShot = MediaWrapper.createView(null, MediaDesc.HALF_THUMBNAIL_SIZE));
            hbox.addChild(_leaveBtn = new CommandButton());
            _leaveBtn.styleName = "closeButton";
            addChild(hbox);
            addChild(_name = FlexUtil.createLabel("", "nameLabel"));
        }
    }

    protected function prepareJoinButton () :void
    {
        if (_joinBtn == null || _joinBtn.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }
            if (_joinBtn == null) {
                _joinBtn = new CommandButton(Msgs.GAME.get("b.join"),
                    LobbyController.JOIN_TABLE, [ _table.tableId, _index ]);
            }
            addChild(_joinBtn);
        }
    }

    protected var _ctx :GameContext;
    protected var _table :Table;
    protected var _index :int;

    protected var _joinBtn :CommandButton;
    protected var _headShot :MediaWrapper;
    protected var _name :Label;
    protected var _leaveBtn :CommandButton;
}
