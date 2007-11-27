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

import com.threerings.msoy.ui.ThumbnailPanel;
import com.threerings.msoy.client.Msgs;

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

        _buttonBox = new HBox();
        _buttonBox.percentWidth = 100;
        _buttonBox.setStyle("horizontalAlign", "right");
        contents.addChild(_buttonBox);

        var create :CommandButton = new CommandButton();
        // we need to have the button go through this function so that the TableConfig and
        // GameConfig are created when the button is pressed
        create.setCallback(function () :void {
            _panel.controller.handleSubmitTable(tconfigger.getTableConfig(), gconf.getGameConfig());
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

    protected var _ctx :GameContext;

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The game item, for configuration reference. */
    protected var _gameDef :GameDefinition;

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;

    protected var _logo :ThumbnailPanel;
    protected var _configBox :Container;
    protected var _buttonBox :HBox;
}
}
