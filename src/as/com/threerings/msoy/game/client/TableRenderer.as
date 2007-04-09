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

        // then add three boxes to contain further content
        addChild(_labelsBox = new VBox());
        _labelsBox.percentWidth = 100;
        var padding :VBox = new VBox();
        padding.setStyle("backgroundColor", 0xF1F4F7);
        padding.width = 2;
        padding.percentHeight = 100;
        addChild(padding);
        var rightSide :VBox = new VBox();
        rightSide.width = 300;
        rightSide.addChild(_seatsGrid = new Tile());
        _seatsGrid.width = 300;
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
                seat = new SeatRenderer();
                _seatsGrid.addChild(seat);
            } else {
                seat = (_seatsGrid.getChildAt(ii) as SeatRenderer);
            }
            seat.update(ctx, table, ii, panel.isSeated());
        }

        // remove any extra seats, should there be any
        while (_seatsGrid.numChildren > length) {
            _seatsGrid.removeChildAt(length);
        }
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

    /** Holds our game's branding background. */
    protected var _background :MediaContainer;

    protected var _watcherCount :Label; // TODO

    protected var _config :Text;

    protected var _labelsBox :VBox;
    protected var _seatsGrid :Tile;
    protected var _buttonsBox :HBox;

    protected var _game :Game;

    protected var _popup :Boolean; 
}
}

import mx.containers.HBox;

import mx.core.UIComponent;

import com.threerings.util.Name;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.client.HeadShotSprite;
import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.data.MsoyTable;

class SeatRenderer extends HBox
{
    public function update (ctx :WorldContext, table :MsoyTable, index :int, 
        weAreSeated :Boolean) :void
    {
        var occupant :Name = (table.occupants[index] as Name);
        var comp :UIComponent = (numChildren > 0)
            ? (getChildAt(0) as UIComponent) : null;

        if ((occupant == null) ||
                (weAreSeated && occupant.equals(ctx.getMemberObject().getVisibleName()))) {
            // we want to show a button
            var btn :CommandButton;
            if (comp is CommandButton) {
                btn = (comp as CommandButton);

            } else {
                if (comp != null) {
                    removeChildAt(0);
                }
                btn = new CommandButton();
                addChildAt(btn, 0);
            }
            if (table.inPlay()) {
                btn.visible = false;

            } else if (occupant == null) {
                btn.visible = true;
                btn.setCommand(LobbyController.SIT, [ table.tableId , index ]);
                btn.label = ctx.xlate("game", "b.sit");
                btn.enabled = !weAreSeated;

            } else {
                btn.visible = true;
                btn.setCommand(LobbyController.LEAVE, table.tableId);
                btn.label = ctx.xlate("game", "b.leave");
                btn.enabled = true;
            }

        } else {
            // we want to show the headshot sprite
            var lbl :HeadShotSprite;
            if (comp is MediaWrapper) {
                lbl = (MediaWrapper(comp).getMediaContainer() as HeadShotSprite);

            } else {
                if (comp != null) {
                    removeChildAt(0);
                }
                lbl = new HeadShotSprite();
                var wrap :MediaWrapper = new MediaWrapper(lbl);
                addChildAt(wrap, 0);
            }
            lbl.setUser(occupant, table.headShots[index] as MediaDesc);
        }
    }
}
