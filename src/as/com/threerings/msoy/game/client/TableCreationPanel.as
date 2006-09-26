package com.threerings.msoy.game.client {

import com.threerings.mx.events.CommandEvent;

import com.threerings.parlor.data.TableConfig;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.game.data.FlashGameConfig;

public class TableCreationPanel extends FloatingPanel
{
    public function TableCreationPanel (ctx :MsoyContext, panel :LobbyPanel)
    {
        super(ctx, ctx.xlate("game", "t.create"));
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
        // TODO
    }

    /**
     * Enact the creation of the table.
     */
    protected function submitTableCreate () :void
    {
        var tableConfig :TableConfig = new TableConfig();
        tableConfig.desiredPlayerCount = 2;
        tableConfig.minimumPlayerCount = 2;

        var gameConfig :FlashGameConfig = new FlashGameConfig();
        gameConfig.configData = "scary meat buckets";

        CommandEvent.dispatch(_panel, LobbyController.SUBMIT_TABLE,
            [ tableConfig, gameConfig ]);
    }

    /** The lobby panel we're in. */
    protected var _panel :LobbyPanel;
}
}
