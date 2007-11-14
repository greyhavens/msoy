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
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.ezgame.client.EZGameConfigurator;
import com.threerings.ezgame.data.GameDefinition;

import com.threerings.msoy.client.Msgs;

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

    public function setEnabled (enabled :Boolean) :void
    {
        if (!enabled) {
            clearCreateGame();
        }
        _configBtn.enabled = enabled;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        styleName = "tableCreationPanel";
        percentWidth = 100;

        _headerBox = new HBox();
        _headerBox.percentWidth = 100;
        _headerBox.setStyle("horizontalAlign", "right");
        var tip :Label = new Label();
        tip.text = Msgs.GAME.get("l.start_tip");
        _headerBox.addChild(tip);

        _configBtn = new CommandButton();
        _configBtn.label = Msgs.GAME.get("b.configure");
        _configBtn.setCallback(function () :void {
            showCreateGame();
            _configBtn.enabled = false;
            _configBtn.visible = false;
            _configBtn.includeInLayout = false;
        });

        _headerBox.addChild(_configBtn);
        addChild(_headerBox);

        // create our various game configuration bits but do not add them
        var gconf :EZGameConfigurator = new EZGameConfigurator();
        gconf.setColumns(3);
        var gconfigger :GameConfigurator = gconf;
        gconfigger.init(_ctx);

        var playersStr :String = Msgs.GAME.get("l.players") + ": ";
        var watchableStr :String = Msgs.GAME.get("l.watchable") + ": ";
        var privateStr :String = Msgs.GAME.get("l.private");
        var match :MsoyMatchConfig = (_gameDef.match as MsoyMatchConfig);

        var tconfigger :TableConfigurator;
        switch (match.getMatchType()) {
        case GameConfig.PARTY:
            tconfigger = new DefaultFlexTableConfigurator(
                -1, -1, -1, true, playersStr, watchableStr, privateStr);
            break;

        case GameConfig.SEATED_GAME:
            // using min_seats for start_seats until we put start_seats in the configuration
            tconfigger = new DefaultFlexTableConfigurator(
                match.minSeats, match.minSeats, match.maxSeats, !match.unwatchable,
                playersStr, Msgs.GAME.get("l.watchable") + ": ");
            break;

        default:
            Log.getLog(this).warning(
                "<match type='" + match.getMatchType() + "'> is not a valid type");
            return;
        }
        tconfigger.init(_ctx, gconfigger);

        var config :MsoyGameConfig = new MsoyGameConfig();
        config.init(_game, _gameDef);
        gconf.setGameConfig(config);

        _configBox = gconf.getContainer();
        _configBox.styleName = "seatsGrid";

        _buttonBox = new HBox();
        _buttonBox.percentWidth = 100;
        _buttonBox.setStyle("horizontalAlign", "right");

        var create :CommandButton = new CommandButton();
        // we need to have the button go through this function so that the TableConfig and
        // GameConfig are created when the button is pressed
        create.setCallback(function () :void {
            _panel.controller.handleSubmitTable(
                tconfigger.getTableConfig(), gconfigger.getGameConfig());
        });
        create.label = Msgs.GAME.get("b.create");
        _buttonBox.addChild(create);

        var cancel :CommandButton = new CommandButton();
        cancel.label = Msgs.GAME.get("b.cancel");
        cancel.setCallback(function () :void {
            clearCreateGame();
        });
        _buttonBox.addChild(cancel);
    }

    protected function showCreateGame () :void
    {
        while (numChildren > 0) {
            removeChildAt(0);
        }
        addChild(_configBox);
        addChild(_buttonBox);
    }

    protected function clearCreateGame () :void
    {
        while (numChildren > 0) {
            removeChildAt(0);
        }
        addChild(_headerBox);
        _configBtn.enabled = true;
        _configBtn.visible = true;
        _configBtn.includeInLayout = true;
    }

    protected var _ctx :GameContext;

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The game item, for configuration reference. */
    protected var _gameDef :GameDefinition;

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;

    protected var _headerBox :HBox;
    protected var _configBtn :CommandButton;
    protected var _configBox :Container;
    protected var _buttonBox :HBox;
}
}
