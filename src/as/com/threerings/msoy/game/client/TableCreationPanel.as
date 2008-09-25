//
// $Id$

package com.threerings.msoy.game.client {

import mx.core.Container;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.TextInput;

import flash.events.Event;

import com.threerings.io.TypedArray;
import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;

import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.data.RangeParameter;
import com.threerings.parlor.data.ToggleParameter;
import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.whirled.game.client.WhirledGameConfigurator;
import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.ui.SimpleGrid;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;

/**
 * Displays an interface for creating a new game table.
 */
public class TableCreationPanel extends VBox
{
    public function TableCreationPanel (ctx :GameContext, ctrl :LobbyController, lobj :LobbyObject)
    {
        _ctx = ctx;
        _ctrl = ctrl;
        _game = lobj.game;
        _gameDef = lobj.gameDef;
    }

    protected function friendToggled (event :Event) :void
    {
        _inviteAll.selected = false;
//
//        // _inviteAll.selected = every friend box selected?
//        _inviteAll.selected = true;
//        for (var i :int = 0; i < _friendsGrid.numChildren; ++i) {
//            if ( ! (_friendsGrid.getCellAt(i) as FriendCheckBox).check.selected) {
//                _inviteAll.selected = false;
//                return;
//            }
//        }
    }

    protected function inviteAllToggled (selected :Boolean) :void
    {
        for (var i :int = 0; i < _friendsGrid.cellCount; ++i) {
            var fcb :FriendCheckBox = _friendsGrid.getCellAt(i) as FriendCheckBox;
            fcb.check.selected = selected;
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        styleName = "tableCreationPanel";
        percentWidth = 100;

        addChild(FlexUtil.createLabel(Msgs.GAME.get("t.create_table"), "lobbyTitle"));

        // create our various game configuration bits but do not add them
        var rparam :ToggleParameter = new ToggleParameter();
        rparam.name = Msgs.GAME.get("l.rated");
        rparam.tip = Msgs.GAME.get("t.rated");
        rparam.start = true;
        var gconf :WhirledGameConfigurator = new WhirledGameConfigurator(rparam);
        gconf.setColumns(1);
        gconf.init(_ctx);

        // add a configuration for the table name (before we give the game 
        var tableName :TextInput = new TextInput();
        tableName.text = Msgs.GAME.get("l.default_table", _ctx.getPlayerObject().getVisibleName());
        tableName.percentWidth = 100;
        gconf.addControl(FlexUtil.createTipLabel(Msgs.GAME.get("l.table"), Msgs.GAME.get("i.table")),
                         tableName);

        var plparam :RangeParameter = new RangeParameter();
        plparam.name = Msgs.GAME.get("l.players");
        plparam.tip = Msgs.GAME.get("t.players");
        var wparam :ToggleParameter = null;
        var pvparam :ToggleParameter = null;

        var match :MsoyMatchConfig = (_gameDef.match as MsoyMatchConfig);
        switch (match.getMatchType()) {
        case GameConfig.PARTY:
            // plparam stays with zeros
            // wparam stays null
            pvparam = new ToggleParameter();
            pvparam.name = Msgs.GAME.get("l.private");
            pvparam.tip = Msgs.GAME.get("t.private");
            break;

        case GameConfig.SEATED_GAME:
            plparam.minimum = match.minSeats;
            plparam.maximum = match.maxSeats;
            plparam.start = match.maxSeats; // game creators don't configure start seats, so use
                                            // the max; they can always start the game early
            if (!match.unwatchable) {
                wparam = new ToggleParameter();
                wparam.name = Msgs.GAME.get("l.watchable");
                wparam.tip = Msgs.GAME.get("t.watchable");
                wparam.start = true;
            }
            // pvparam stays null
            break;

        default:
            Log.getLog(this).warning(
                "<match type='" + match.getMatchType() + "'> is not a valid type");
            return;
        }

        var tconfigger :TableConfigurator =
            new MsoyTableConfigurator(plparam, wparam, pvparam, tableName);
        tconfigger.init(_ctx, gconf);

        var config :MsoyGameConfig = new MsoyGameConfig();
        config.init(_game, _gameDef);
        gconf.setGameConfig(config);

        _configBox = gconf.getContainer();
        _configBox.percentWidth = 100;
        addChild(_configBox);

        // add an interface for inviting friends to play
        addChild(FlexUtil.createLabel(Msgs.GAME.get("l.invite_friends"), "lobbyTitle"));
        // TODO: turn this into an action label
        _inviteAll = new CommandCheckBox(Msgs.GAME.get("l.invite_all"), inviteAllToggled);
        _inviteAll.styleName = "lobbyLabel";
        // TODO: add with label to HBox

        var onlineFriends :Array = _ctx.getOnlineFriends();
        if (onlineFriends.length ==  0) {
            addChild(FlexUtil.createLabel(Msgs.GAME.get("l.invite_no_friends")));

        } else {
            var columns :int = Math.min(FRIENDS_GRID_COLUMNS, onlineFriends.length);
            _friendsGrid = new SimpleGrid(columns);
            _friendsGrid.setStyle("horizontalGap", 5);
            for each (var friend :FriendEntry in onlineFriends) {
                var fcb :FriendCheckBox = new FriendCheckBox(friend);
                fcb.check.addEventListener(Event.CHANGE, friendToggled);
                _friendsGrid.addCell(fcb);
            }
            addChild(_friendsGrid);
        }

        // only show invite all if we have more than one friend to invite
        _inviteAll.visible = (onlineFriends.length > 1);

        // finally add buttons for create and cancel
        var bottomRow :HBox = new HBox();
        bottomRow.percentWidth = 100;
        bottomRow.setStyle("horizontalAlign", "right");
        bottomRow.addChild(new CommandButton(Msgs.GAME.get("b.cancel"), function () : void {
            _ctrl.panel.setMode(_ctrl.haveActionableTables() ?
                LobbyController.MODE_MATCH : LobbyController.MODE_SPLASH);
        }));
        bottomRow.addChild(new CommandButton(Msgs.GAME.get("b.create"),
            createGame, [ tconfigger, gconf ]));
        addChild(bottomRow);
    }

    protected function createGame (tconf :TableConfigurator, gconf :GameConfigurator) :void
    {
        var invIds :TypedArray = TypedArray.create(int);
        if (_friendsGrid != null) {
            for (var ii :int = 0; ii < _friendsGrid.cellCount; ii++) {
                var fcb :FriendCheckBox = (_friendsGrid.getCellAt(ii) as FriendCheckBox);
                if (fcb.check.selected) {
                    invIds.push(fcb.friend.name.getMemberId());
                }
            }
        }
        _ctrl.handleSubmitTable(tconf.getTableConfig(), gconf.getGameConfig(), invIds);
    }

    protected var _ctx :GameContext;
    protected var _ctrl :LobbyController;

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The game item, for configuration reference. */
    protected var _gameDef :GameDefinition;

    protected var _configBox :Container;
    protected var _friendsGrid :SimpleGrid;
    protected var _inviteAll :CommandCheckBox;

    protected static const FRIENDS_GRID_COLUMNS :int = 6;
}
}

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.VBox;
import mx.controls.CheckBox;
import mx.controls.Label;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.ui.MediaWrapper;

class FriendCheckBox extends VBox
{
    public var friend :FriendEntry;
    public var check :CheckBox;

    public function FriendCheckBox (friend :FriendEntry)
    {
        styleName = "friendCheckBox";
        this.friend = friend;

        addChild(MediaWrapper.createView(friend.photo, MediaDesc.HALF_THUMBNAIL_SIZE));
        var name :Label = FlexUtil.createLabel(friend.name.toString());
        name.maxWidth = 2*MediaDesc.THUMBNAIL_WIDTH/3;
        addChild(name);
        addChild(check = new CheckBox());
        check.width = 14; // don't ask; go punch someone at adobe instead

        addEventListener(MouseEvent.CLICK, handleClick);
    }

    // allow all kinds of sloppy clicking to toggle the checkbox
    protected function handleClick (event :MouseEvent) :void
    {
        if (event.target != check) { // because the checkbox will have already handled it
            check.selected = !check.selected;
            check.dispatchEvent(new Event(Event.CHANGE));
        }
    }
}
