package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.controls.Button;

import com.threerings.msoy.client.Msgs
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.util.CommandEvent;

public class FloatingTableDisplay extends FloatingPanel
{
    public static const BACK_TO_LOBBY_BUTTON :int = 100;

    public function FloatingTableDisplay (ctx :WorldContext, panel :LobbyPanel)
    {
        super(ctx, Msgs.GAME.get("t.table_display"));
        _panel = panel;

        addButtons(BACK_TO_LOBBY_BUTTON);
    }

    override public function open (modal :Boolean = false, parent :DisplayObject = null,
        avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);
        x = 10;
        y = 10;
    }

    override protected function createButton (buttonId :int) :Button
    {
        var btn :Button;
        switch (buttonId) {
        case BACK_TO_LOBBY_BUTTON:
            btn = new Button();
            btn.label = Msgs.GAME.get("b.back_to_lobby");
            break;

        default:
            btn = super.createButton(buttonId);
        }
        return btn;
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        switch(buttonId) {
        case BACK_TO_LOBBY_BUTTON:
            CommandEvent.dispatch(_panel, LobbyController.JOIN_LOBBY);
            close();
            break;

        default:
            super.buttonClicked(buttonId);
        }
    }

    protected var _panel :LobbyPanel;
}
}
