//
// $Id$

package com.threerings.msoy.game.client {

import mx.core.Container;
import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;
import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

import com.threerings.parlor.client.DefaultFlexTableConfigurator;
import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.ezgame.client.EZGameConfigurator;
import com.threerings.ezgame.data.GameDefinition;
import com.threerings.ezgame.data.RangeParameter;
import com.threerings.ezgame.data.ToggleParameter;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.ui.SimpleGrid;
import com.threerings.msoy.ui.ThumbnailPanel;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;

public class TableCreationPanel extends HBox
{
    public function TableCreationPanel (ctx :GameContext, panel :LobbyPanel)
    {
        _ctx = ctx;
        _game = panel.getGame();
        _gameDef = panel.getGameDefinition();
        _panel = panel;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        styleName = "tableCreationPanel";
        percentWidth = 100;

        addChild(_logo = new ThumbnailPanel());
        _logo.setItem(_game);

        var contents :VBox = new VBox();
        contents.percentWidth = 100;
        addChild(contents);

        // create our various game configuration bits but do not add them
        var rparam :ToggleParameter = new ToggleParameter();
        rparam.name = Msgs.GAME.get("l.rated");
        rparam.tip = Msgs.GAME.get("t.rated");
        rparam.start = true;
        var gconf :EZGameConfigurator = new EZGameConfigurator(rparam);
        gconf.setColumns(3);
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
            plparam.start = match.startSeats;
            plparam.maximum = match.maxSeats;
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
            new DefaultFlexTableConfigurator(plparam, wparam, pvparam);
        tconfigger.init(_ctx, gconf);

        var config :MsoyGameConfig = new MsoyGameConfig();
        config.init(_game, _gameDef);
        gconf.setGameConfig(config);

        _configBox = gconf.getContainer();
        _configBox.styleName = "seatsGrid";
        contents.addChild(_configBox);

        var bottomRow :HBox = new HBox();
        bottomRow.percentWidth = 100;
        bottomRow.setStyle("verticalAlign", "bottom");

        // add an interface for inviting friends to play
        var friendsBox :VBox = new VBox();
        friendsBox.percentWidth = 100;
        friendsBox.setStyle("verticalGap", 0);
        var onlineFriends :Array = _ctx.getWorldContext().getMemberObject().friends.toArray().filter(
            function (friend :FriendEntry, index :int, array :Array) :Boolean {
                return friend.online;
            });
        if (onlineFriends.length ==  0) {
            friendsBox.addChild(MsoyUI.createLabel(Msgs.GAME.get("l.invite_no_friends")));
        } else {
            friendsBox.addChild(MsoyUI.createLabel(Msgs.GAME.get("l.invite_friends"), "lobbyLabel"));
            _friendsGrid = new SimpleGrid(FRIENDS_GRID_COLUMNS);
            _friendsGrid.setStyle("horizontalGap", 15);
            for each (var friend :FriendEntry in onlineFriends) {
                _friendsGrid.addCell(new FriendCheckBox(friend));
            }
            friendsBox.addChild(_friendsGrid);
        }
        bottomRow.addChild(friendsBox);

        // finally add buttons for create and cancel
        _buttonBox = new HBox();
        bottomRow.addChild(_buttonBox);
        contents.addChild(bottomRow);

        var create :CommandButton = new CommandButton();
        // we need to have the button go through this function so that the TableConfig and
        // GameConfig are created when the button is pressed
        create.setCallback(function () :void {
            createGame(tconfigger, gconf);
        });
        create.label = Msgs.GAME.get("b.create");
        _buttonBox.addChild(create);

        var cancel :CommandButton = new CommandButton();
        cancel.label = Msgs.GAME.get("b.cancel");
        cancel.setCallback(function () :void {
            _panel.hideCreateGame();
        });
        _buttonBox.addChild(cancel);
    }

    protected function createGame (tconf :TableConfigurator, gconf :GameConfigurator) :void
    {
        var friends :Array = [];
        if (_friendsGrid != null) {
            for (var ii :int = 0; ii < _friendsGrid.cellCount; ii++) {
                var fcb :FriendCheckBox = (_friendsGrid.getCellAt(ii) as FriendCheckBox);
                if (fcb.checked) {
                    friends.push(fcb.friend.name);
                }
            }
        }
        _panel.controller.handleSubmitTable(tconf.getTableConfig(), gconf.getGameConfig(), friends);
    }

    protected var _ctx :GameContext;

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The game item, for configuration reference. */
    protected var _gameDef :GameDefinition;

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;

    protected var _logo :ThumbnailPanel;
    protected var _configBox :Container;
    protected var _friendsGrid :SimpleGrid;
    protected var _buttonBox :HBox;

    protected static const FRIENDS_GRID_COLUMNS :int = 6;
}
}

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.CheckBox;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.ui.ThumbnailPanel;

class FriendCheckBox extends VBox
{
    public var friend :FriendEntry;

    public function FriendCheckBox (friend :FriendEntry)
    {
        styleName = "friendCheckBox";
        this.friend = friend;

        var row :HBox = new HBox();
        row.setStyle("horizontalGap", 4);
        var thumb :ThumbnailPanel = new ThumbnailPanel(MediaDesc.HALF_THUMBNAIL_SIZE);
        thumb.setMediaDesc(friend.photo);
        row.addChild(thumb);
        row.addChild(_check = new CheckBox());
        _check.width = 14; // don't ask; go punch someone at adobe instead
        addChild(row);
        addChild(MsoyUI.createLabel(friend.name.toString()));
    }

    public function get checked () :Boolean
    {
        return _check.selected;
    }

    protected var _check :CheckBox;
}
