package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.Tile;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.controls.Label;
import mx.controls.Text;

import com.threerings.util.Name;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.CommandButton;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.ezgame.data.EZGameConfig;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.MsoyTable;

public class TableRenderer extends HBox
{
    /** The context, initialized by our ClassFactory. */
    public var ctx :WorldContext;

    /** The panel we're rendering to. */
    public var panel :LobbyPanel;

    public function TableRenderer (popup :Boolean = false)
    {
        super();
        _popup = popup
        if (!_popup) {
            // when used in a List, we should not be included in the layout
            includeInLayout = false;
        }
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _game = panel.getGame();

        setStyle("horizontalGap", 0);
        setStyle("verticalGap", 0);

        addChild(_labelsBox = new VBox());
        _labelsBox.width = CONFIG_WIDTH;
        var padding :VBox = new VBox();
        padding.setStyle("backgroundColor", 0xF1F4F7);
        padding.width = PADDING_WIDTH;
        padding.percentHeight = 100;
        addChild(padding);
        var rightSide :VBox = new VBox();
        rightSide.percentWidth = 100;
        rightSide.addChild(_seatsGrid = new Tile());
        _seatsGrid.percentWidth = 100;
        _seatsGrid.verticalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.horizontalScrollPolicy = ScrollPolicy.OFF;
        rightSide.addChild(_buttonsBox = new HBox());
        _buttonsBox.percentWidth = 100;
        addChild(rightSide);

        _watcherCount = new Label();
        _config = new Text();
        _labelsBox.addChild(_watcherCount);
        _labelsBox.addChild(_config);
    }

    override public function set data (newData :Object) :void
    {
        super.data = newData;

        recheckTable();
    }

    override public function set width (width :Number) :void
    {
        super.width = width;
        if (_seatsGrid != null) {
            _seatsGrid.width = width - CONFIG_WIDTH - PADDING_WIDTH;
        }
    }

    /** 
     * Get the amount of width we could use if we had the room.
     */
    public function get maxUsableWidth () :int
    {
        return _maxUsableWidth;
    }

    protected function recheckTable () :void
    {
        var table :MsoyTable = (data as MsoyTable);
        if (table == null) {
            return;
        }

        // TODO
        _watcherCount.text = "Watchers: " + table.watcherCount;

        // update the seats
        if (table.occupants != null) {
            updateSeats(table);
        }

        updateButtons(table);

        updateConfig(table);
    }

    protected function updateSeats (table :MsoyTable) :void
    {
        var length :int = table.occupants.length;
        for (var ii :int = 0; ii < length; ii++) {
            var seat :SeatRenderer;
            if (_seatsGrid.numChildren <= ii) {
                seat = new SeatRenderer(ctx, table, ii);
                _seatsGrid.addChild(seat);
            } else {
                seat = (_seatsGrid.getChildAt(ii) as SeatRenderer);
            }
            seat.update();
        }

        // remove any extra seats, should there be any
        while (_seatsGrid.numChildren > length) {
            _seatsGrid.removeChildAt(length);
        }

        _seatsGrid.validateNow();
        _maxUsableWidth = _seatsGrid.measuredMinWidth * _seatsGrid.numChildren + CONFIG_WIDTH +
            PADDING_WIDTH + 10 * _seatsGrid.numChildren; // this won't be needed in the end
    }

    protected function updateButtons (table :MsoyTable) :void
    {
        // remove all, then re-add
        for (var ii :int = _buttonsBox.numChildren - 1; ii >= 0; ii--) {
            _buttonsBox.removeChildAt(ii);
        }

        var btn :CommandButton;

        // if we are the creator, add a button for starting the game now
        if (table.occupants != null && table.occupants.length > 0 &&
                ctx.getMemberObject().getVisibleName().equals(table.occupants[0]) &&
                (table.tconfig.desiredPlayerCount > table.tconfig.minimumPlayerCount)) {

            btn = new CommandButton(LobbyController.START_TABLE, table.tableId);
            btn.label = ctx.xlate("game", "b.start_now");
            btn.enabled = table.mayBeStarted();
            _buttonsBox.addChild(btn);
        }

        // if we're in a popup, add a button to return to the lobby
        if (_popup) {
            btn = new CommandButton(LobbyController.JOIN_LOBBY);
            btn.label = ctx.xlate("game", "b.back_to_lobby");
            _buttonsBox.addChild(btn);
        }

        // maybe add a button for entering the game
        if (table.gameOid != -1) {
            var key :String = null;
            switch (table.config.getGameType()) {
            default:
                if (!_game.getGameDefinition().unwatchable) {
                    key = "b.watch";
                }
                break;

            case GameConfig.SEATED_CONTINUOUS:
                key = "b.enter";
                break;

            case GameConfig.PARTY:
                key = "b.join";
                break;
            }

            if (key != null) {
                btn = new CommandButton(MsoyController.GO_LOCATION, table.gameOid);
                btn.label = ctx.xlate("game", key);
                _buttonsBox.addChild(btn);
            }
        }
    }

    /**
     * Update the displayed custom configuration options.
     */
    protected function updateConfig (table :MsoyTable) :void
    {
        var customConfig :Object = null;
        if (table.config is EZGameConfig) {
            customConfig = (table.config as EZGameConfig).customConfig;
        }

        // TODO: Ray: re-parse XML and show the options in the
        // right order, with the right names, and the tooltips
        var config :String = "";
        if (customConfig != null) {
            for (var s :String in customConfig) {
                if (config != "") {
                    config += "\n";
                }
                config += s + ": " + customConfig[s];
            }
        }
        _config.text = config;
    }

    protected static const CONFIG_WIDTH :int = 100;
    protected static const PADDING_WIDTH :int = 2;

    /** Holds our game's branding background. */
    protected var _background :MediaContainer;

    protected var _watcherCount :Label; // TODO

    protected var _config :Text;

    protected var _labelsBox :VBox;
    protected var _seatsGrid :Tile;
    protected var _buttonsBox :HBox;

    protected var _game :Game;

    protected var _popup :Boolean; 

    protected var _maxUsableWidth :int;
}
}

import mx.containers.HBox;

import mx.controls.Label;

import mx.core.UIComponent;

import com.threerings.util.Name;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.data.MsoyTable;

class SeatRenderer extends HBox
{
    public function SeatRenderer (ctx :WorldContext, table :MsoyTable, index :int) 
    {
        _ctx = ctx;
        _table = table;
        _index = index;
    }

    public function update () :void
    {
        var occupant :Name = (_table.occupants[_index] as Name);

        if (occupant != null) {
            prepareForOccupant();
            _headShot.setMedia((_table.headShots[_index] as MediaDesc).getMediaPath());
            _name.text = occupant.toString();
        } else {
            prepareJoinButton();
        }

    }

    protected function prepareForOccupant () :void
    {
        if (_name == null || _name.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }
            _headShot = new MediaContainer();
            var wrapper :MediaWrapper = new MediaWrapper(_headShot);
            wrapper.maxWidth = 40;
            wrapper.maxHeight = 40;
            addChild(wrapper);
            _name = new Label();
            addChild(_name);
            _leaveBtn = new CommandButton(LobbyController.LEAVE_TABLE);
            _leaveBtn.styleName = "closeButton";
            addChild(_leaveBtn);
        } 
    }

    protected function prepareJoinButton () :void
    {
        if (_joinBtn == null || _joinBtn.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }
            if (_joinBtn == null) {
                _joinBtn = new CommandButton(LobbyController.JOIN_TABLE, 
                    [ _table.tableId, _index ]);
                _joinBtn.label = _ctx.xlate("game", "b.join");
            }
            addChild(_joinBtn);
        }
    }

    protected var _ctx :WorldContext;
    protected var _table :MsoyTable;
    protected var _index :int;

    protected var _joinBtn :CommandButton;
    protected var _headShot :MediaContainer;
    protected var _name :Label;
    protected var _leaveBtn :CommandButton;
}
