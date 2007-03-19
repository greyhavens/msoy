package com.threerings.msoy.game.client {

import com.threerings.util.CommandEvent;

import com.threerings.parlor.data.TableConfig;

import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.client.DefaultFlexTableConfigurator;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.game.client.GameConfigurator;

import com.threerings.ezgame.client.EZGameConfigurator;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.GameDefinition;

public class TableCreationPanel extends FloatingPanel
{
    public function TableCreationPanel (
        ctx :WorldContext, game :Game, panel :LobbyPanel)
    {
        super(ctx, Msgs.GAME.get("t.create", game.name));
        _game = game;
        _panel = panel;
        showCloseButton = true;

        open(true, panel);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        createConfigInterface();

        // add the standard buttons
        addButtons(CANCEL_BUTTON, OK_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        switch (buttonId) {
        case OK_BUTTON:
            submitTableCreate();
            break;

        case CANCEL_BUTTON:
            _panel.createBtn.enabled = true;
            break;
        }

        super.buttonClicked(buttonId);
    }

    /**
     * Create the table / game configuration interface.
     */
    protected function createConfigInterface () :void
    {
        var gconf :EZGameConfigurator = new EZGameConfigurator();
        _gconfigger = gconf;
        _gconfigger.init(_ctx);
        var gameDef :GameDefinition = _game.getGameDefinition();
        gconf.setXMLConfig(gameDef.config);
        if (gameDef.gameType == GameConfig.PARTY) {
            _tconfigger = new DefaultFlexTableConfigurator(-1, -1, -1, true);
        } else if (gameDef.gameType == GameConfig.SEATED_GAME)  {
            // using min_seats for start_seats until we put start_seats in the configuration
            _tconfigger = new DefaultFlexTableConfigurator(gameDef.minSeats, gameDef.minSeats,
                gameDef.maxSeats, true);
        } else { 
            Log.getLog(this).warning("<match type='" + gameDef.gameType + "'> is not a valid type");
            return;
        }
        _tconfigger.init(_ctx, _gconfigger);

        var config :MsoyGameConfig = new MsoyGameConfig();
        config.gameMedia = _game.gameMedia.getMediaPath();
        config.persistentGameId = _game.getPrototypeId();
        config.gameType = gameDef.gameType;
        gconf.setGameConfig(config);

        addChild(gconf.getContainer());
    }

    /**
     * Enact the creation of the table.
     */
    protected function submitTableCreate () :void
    {
        CommandEvent.dispatch(_panel, LobbyController.SUBMIT_TABLE,
            [ _tconfigger.getTableConfig(), _gconfigger.getGameConfig() ]);
    }

    /** The game item, for configuration reference. */
    protected var _game :Game;

    /** The table configurator. */
    protected var _tconfigger :TableConfigurator;

    /** The game configurator. */
    protected var _gconfigger :GameConfigurator;

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;
}
}
