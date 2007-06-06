//
// $Id$

package com.threerings.msoy.game.client {

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.flex.CommandButton;
import com.threerings.util.CommandEvent;

import com.threerings.parlor.data.TableConfig;

import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.client.DefaultFlexTableConfigurator;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.game.client.GameConfigurator;

import com.threerings.ezgame.client.EZGameConfigurator;
import com.threerings.ezgame.data.GameDefinition;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;

public class TableCreationPanel extends HBox
{
    public function TableCreationPanel (ctx :WorldContext, panel :LobbyPanel)
    {
        _ctx = ctx;
        _game = panel.getGame();
        _gameDef = panel.getGameDefinition();
        _panel = panel;
    }

    public function getCreateButton () :CommandButton
    {
        return _createBtn;
    }

    override protected function createChildren () :void
    {
        styleName = "tableCreationPanel";
        percentWidth = 100;
        percentHeight = 100;

        var gconf :EZGameConfigurator = new EZGameConfigurator();
        var gconfigger :GameConfigurator = gconf;
        gconfigger.init(_ctx);

        var match :MsoyMatchConfig = (_gameDef.match as MsoyMatchConfig);
        var tconfigger :TableConfigurator;
        switch (match.getMatchType()) {
        case GameConfig.PARTY:
            tconfigger = new DefaultFlexTableConfigurator(-1, -1, -1, true);
            break;

        case GameConfig.SEATED_GAME:
            // using min_seats for start_seats until we put start_seats in the configuration
            tconfigger = new DefaultFlexTableConfigurator(
                match.minSeats, match.minSeats, match.maxSeats, !match.unwatchable,
                Msgs.GAME.get("l.players"), Msgs.GAME.get("l.watchable"));
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

        _createBtn = new CommandButton();
        // We need to have the button go through this function so that
        // the TableConfig and GameConfig are created when the button is pressed.
        _createBtn.setCallback(function () :void {
            _panel.controller.handleSubmitTable(
                tconfigger.getTableConfig(), gconfigger.getGameConfig());
        });
        _createBtn.label = Msgs.GAME.get("b.create");
        var btnBox :HBox = new HBox();
        btnBox.percentWidth = 100;
        btnBox.percentHeight = 100;
        btnBox.setStyle("verticalAlign", "middle");
        btnBox.setStyle("horizontalAlign", "center");
        btnBox.addChild(_createBtn);
        gconf.getContainer().addChild(btnBox);

        addChild(gconf.getContainer());
    }

    protected var _ctx :WorldContext;

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The game item, for configuration reference. */
    protected var _gameDef :GameDefinition;

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;

    protected var _createBtn :CommandButton;
}
}
