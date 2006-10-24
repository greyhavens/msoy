package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.TitleWindow;

import mx.controls.Button;
import mx.controls.Label;

import mx.managers.PopUpManager;

import com.threerings.parlor.game.client.FlexGameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberName;

import com.threerings.msoy.game.data.FlashGameConfig;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * A panel for configuring a game invitation to another player.
 */
public class InvitePanel extends FloatingPanel
{
    public function InvitePanel (ctx :MsoyContext, invitee :MemberName)
    {
        super(ctx, Msgs.GAME.get("t.inviteGame", invitee));
        _invitee = invitee;

        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // set up the actual configurator
        // TODO
        var gc :GameConfig = new FlashGameConfig();
        _configger = FlexGameConfigurator(gc.createConfigurator());
        _configger.init(_ctx);
        _configger.setGameConfig(gc);
        addChild(_configger.getContainer());

        addButtons(CANCEL_BUTTON, OK_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            var gc :GameConfig = _configger.getGameConfig();
            _ctx.getGameDirector().sendInvite(_invitee, gc);
        }

        super.buttonClicked(buttonId); // will close() on OK or CANCEL
    }

    protected var _invitee :MemberName;

    protected var _configger :FlexGameConfigurator;
}
}
