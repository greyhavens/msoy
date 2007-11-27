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

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTable;

public class TableRenderer extends VBox
{
    /** The game context, initialized by our ClassFactory. */
    public var gctx :GameContext;

    /** The panel we're rendering to. */
    public var panel :LobbyPanel;

    public function TableRenderer (popup :Boolean = false)
    {
        super();
        _popup = popup;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        percentWidth = 100;
    }

    override public function set data (newData :Object) :void
    {
        super.data = newData;
        recheckTable();
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        createSeats();
    }

    protected function removeChildren () :void
    {
        while (numChildren > 0) {
            removeChild(getChildAt(0));
        }
    }

    protected function recheckTable () :void
    {
        if (gctx.getPlayerObject() == null) { // if we're logged off, don't worry
            return;
        }

        if (data is String) {
            removeChildren();
            var type :String = (data as String).substr(0, 1);
            var message :String = (data as String).substr(1);
            var label :Label = new Label();
            label.text = message;
            if (type == "H") {
                styleName = "tableHeader";
                label.setStyle("fontWeight", "bold");
            } else {
                styleName = "tableMessage";
            }
            addChild(label);
            return;

        } else if (getChildAt(0) is Label) {
            removeChildren();
            createSeats();
        }

        var table :MsoyTable = (data as MsoyTable);
        if (table == null) {
            Log.getLog(this).warning("Got null table in renderer? [data=" + data + "].");
            return;
        }

        if (!_popup) {
            // TODO: table.playerCount is not getting set... I'm not sure why TableManager isn't
            // doing this, but I don't want to mess with that, in case one of the other games is
            // relying on the current behavior.
            _watcherCount = table.watcherCount;
            // _watcherCount = table.watcherCount - table.playerCount;
        }

        // update the seats
        var length :int = table.occupants.length;
        if (length != 0) {
            updateSeats(table, length);
        }
        // remove any extra seats/buttons, should there be any
        while (_seatsGrid.numChildren > length) {
            _seatsGrid.removeChildAt(length);
        }
        updateButtons(table);
        _seatsGrid.validateNow();

        if (!_popup) {
            updateConfig(table);
        }
    }

    protected function createSeats () :void
    {
        _game = panel.getGame();
        _gameDef = panel.getGameDefinition();

        if (_popup) {
            styleName = "floatingTableRenderer";
        } else {
            styleName = "listTableRenderer";
        }

        addChild(_seatsGrid = new HBox());
        _seatsGrid.verticalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.horizontalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.styleName = "seatsGrid";

        if (!_popup) {
            _labelsBox = new HBox();
            _labelsBox.verticalScrollPolicy = ScrollPolicy.OFF;
            _labelsBox.horizontalScrollPolicy = ScrollPolicy.OFF;
            addChild(_labelsBox);
        }
    }

    protected function updateSeats (table :MsoyTable, length :int) :void
    {
        for (var ii :int = 0; ii < length; ii++) {
            var seat :SeatRenderer;
            if (_seatsGrid.numChildren <= ii) {
                seat = new SeatRenderer();
                _seatsGrid.addChild(seat);
            } else if (!(_seatsGrid.getChildAt(ii) is SeatRenderer)) {
                seat = new SeatRenderer();
                _seatsGrid.addChildAt(seat, ii);
            } else {
                seat = (_seatsGrid.getChildAt(ii) as SeatRenderer);
            }
            seat.update(gctx, table, ii, panel.isSeated());
        }
        _seatsGrid.setStyle("horizontalGap", _popup ? 10 : 10*(Math.max(1, 8-length)));
    }

    protected function updateButtons (table :MsoyTable) :void
    {
        var btn :CommandButton;

        // if we are the creator, add a button for starting the game now
        if (table.occupants.length > 0 &&
            gctx.getPlayerObject().getVisibleName().equals(table.occupants[0]) &&
            (table.tconfig.desiredPlayerCount > table.tconfig.minimumPlayerCount)) {
            btn = new CommandButton(LobbyController.START_TABLE, table.tableId);
            btn.label = Msgs.GAME.get("b.start_now");
            btn.enabled = table.mayBeStarted();
            _seatsGrid.addChild(btn);
        }

        // update our background color based on whether or not we're running
        setStyle("backgroundColor", (table.gameOid > 0) ? 0xEEEEEE : 0xFFFFFF);

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
                btn = new CommandButton(MsoyController.GO_GAME, [ _game.gameId, table.gameOid ]);
                btn.label = Msgs.GAME.get(key);
                _seatsGrid.addChild(btn);
            }
        }
    }

    /**
     * Update the displayed custom configuration options.
     */
    protected function updateConfig (table :MsoyTable) :void
    {
        while (_labelsBox.numChildren > 0) {
            _labelsBox.removeChild(_labelsBox.getChildAt(0));
        }

        // if the game is in progress, report its watcher count
        if (table.gameOid != -1) {
            var wc :String = String(_watcherCount);
            if (table.config.getMatchType() == GameConfig.PARTY) {
                _labelsBox.addChild(makeConfigLabel(Msgs.GAME.get("l.players"), wc));
            } else if (!(_gameDef.match as MsoyMatchConfig).unwatchable &&
                       !table.tconfig.privateTable) {
                _labelsBox.addChild(makeConfigLabel(Msgs.GAME.get("l.watchers"), wc));
            }
        }

        if (table.config is EZGameConfig) {
            var params :Array = (table.config as EZGameConfig).getGameDefinition().params;
            if (params != null) {
                var ezconfig :EZGameConfig = (table.config as EZGameConfig);
                for each (var param :Parameter in params) {
                    var name :String = StringUtil.isBlank(param.name) ? param.ident : param.name;
                    var value :String = String(ezconfig.params.get(param.ident));
                    _labelsBox.addChild(makeConfigLabel(name, value, param.tip));
                }
            }
        }
    }

    protected function makeConfigLabel (name :String, value :String, tip :String = "") :UIComponent
    {
        var label :Label = MsoyUI.createLabel(name + ": " + value, "lobbyLabel");
        if (tip != "") {
            label.toolTip = tip;
        }
        return label;
    }

    protected var _watcherCount :int;
    protected var _labelsBox :HBox;
    protected var _seatsGrid :HBox;

    protected var _game :Game;
    protected var _gameDef :GameDefinition;

    protected var _popup :Boolean; 
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

class SeatRenderer extends VBox
{
    public function SeatRenderer () :void
    {
        styleName = "seatRenderer";
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
