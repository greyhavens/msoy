//
// $Id$

package com.threerings.msoy.game.client {

import mx.core.Container;
import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.io.TypedArray;
import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
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

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;

public class TableCreationPanel extends VBox
{
    public function TableCreationPanel (ctx :GameContext, panel :LobbyPanel)
    {
        _ctx = ctx;
        _game = panel.getGame();
        _gameDef = panel.getGameDefinition();
        _panel = panel;
    }

    // I could spend hours figuring out how Flex notifies us when we're added to and removed from
    // the heirarchy and then discover bugs in that and spend more hours debugging it, or I can
    // just call this manually...
    public function updateOnlineFriends () :void
    {
        _friendsBox.removeAllChildren();
        var onlineFriends :Array = _ctx.getOnlineFriends();
        if (onlineFriends.length ==  0) {
            _friendsBox.addChild(FlexUtil.createLabel(Msgs.GAME.get("l.invite_no_friends")));
        } else {
            var columns :int = Math.min(FRIENDS_GRID_COLUMNS, onlineFriends.length);
            _friendsGrid = new SimpleGrid(columns);
            _friendsGrid.setStyle("horizontalGap", 5);
            for each (var friend :FriendEntry in onlineFriends) {
                _friendsGrid.addCell(new FriendCheckBox(friend));
            }
            _friendsBox.addChild(_friendsGrid);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        styleName = "tableCreationPanel";
        percentWidth = 100;

        var row :HBox = new HBox();
        row.percentWidth = 100;
        addChild(row);

        // create our various game configuration bits but do not add them
        var rparam :ToggleParameter = new ToggleParameter();
        rparam.name = Msgs.GAME.get("l.rated");
        rparam.tip = Msgs.GAME.get("t.rated");
        rparam.start = true;
        var gconf :WhirledGameConfigurator = new WhirledGameConfigurator(rparam);
        gconf.setColumns(1);
        gconf.init(_ctx);

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

        var tconfigger :TableConfigurator = new MsoyTableConfigurator(plparam, wparam, pvparam);
        tconfigger.init(_ctx, gconf);

        var config :MsoyGameConfig = new MsoyGameConfig();
        config.init(_game, _gameDef);
        gconf.setGameConfig(config);

        _configBox = gconf.getContainer();
        _configBox.styleName = "configBox";
        row.addChild(wrapBox(Msgs.GAME.get("l.config_game"), _configBox));

        // add an interface for inviting friends to play
        _friendsBox = new VBox();
        _friendsBox.styleName = "friendsBox";
        _friendsBox.percentWidth = 100;
        _friendsBox.percentHeight = 100;
        _friendsBox.setStyle("verticalGap", 0);
        row.addChild(wrapBox(Msgs.GAME.get("l.invite_friends"), _friendsBox, true));

        // finally add buttons for create and cancel
        var bottomRow :HBox = new HBox();
        bottomRow.percentWidth = 100;
        bottomRow.setStyle("horizontalAlign", "right");
        bottomRow.addChild(
            new CommandButton(Msgs.GAME.get("b.create"), createGame, [ tconfigger, gconf ]));
        bottomRow.addChild(new CommandButton(Msgs.GAME.get("b.cancel"), _panel.showTables));
        addChild(bottomRow);
    }

    protected function wrapBox (title :String, contents :Container, hstretch :Boolean = false) :VBox
    {
        var wrapper :VBox = new VBox();
        wrapper.setStyle("verticalGap", 0);
        wrapper.addChild(FlexUtil.createLabel(title));
        wrapper.addChild(contents);
        if (hstretch) {
            wrapper.percentWidth = 100;
        }
        wrapper.percentHeight = 100;
        return wrapper;
    }

    protected function createGame (tconf :TableConfigurator, gconf :GameConfigurator) :void
    {
        var invIds :TypedArray = TypedArray.create(int);
        if (_friendsGrid != null) {
            for (var ii :int = 0; ii < _friendsGrid.cellCount; ii++) {
                var fcb :FriendCheckBox = (_friendsGrid.getCellAt(ii) as FriendCheckBox);
                if (fcb.checked) {
                    invIds.push(fcb.friend.name.getMemberId());
                }
            }
        }
        _panel.controller.handleSubmitTable(tconf.getTableConfig(), gconf.getGameConfig(), invIds);
    }

    protected var _ctx :GameContext;

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The game item, for configuration reference. */
    protected var _gameDef :GameDefinition;

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;

    protected var _configBox :Container;
    protected var _friendsBox :VBox;
    protected var _friendsGrid :SimpleGrid;

    protected static const FRIENDS_GRID_COLUMNS :int = 6;
}
}

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

    public function FriendCheckBox (friend :FriendEntry)
    {
        styleName = "friendCheckBox";
        this.friend = friend;

        addChild(MediaWrapper.createView(friend.photo, MediaDesc.HALF_THUMBNAIL_SIZE));
        var name :Label = FlexUtil.createLabel(friend.name.toString());
        name.maxWidth = 2*MediaDesc.THUMBNAIL_WIDTH/3;
        addChild(name);
        addChild(_check = new CheckBox());
        _check.width = 14; // don't ask; go punch someone at adobe instead

        addEventListener(MouseEvent.CLICK, handleClick);
    }

    public function get checked () :Boolean
    {
        return _check.selected;
    }

    // allow all kinds of sloppy clicking to toggle the checkbox
    protected function handleClick (event :MouseEvent) :void
    {
        if (event.target != _check) { // because the checkbox will have already handled it
            _check.selected = !_check.selected;
        }
    }

    protected var _check :CheckBox;
}
