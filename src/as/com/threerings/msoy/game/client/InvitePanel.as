package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Button;
import mx.controls.Label;

import mx.managers.PopUpManager;

import com.threerings.parlor.game.client.FlexGameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberName;

import com.threerings.msoy.game.data.FlashGameConfig;

/**
 * A panel for configuring a game invitation to another player.
 */
public class InvitePanel extends VBox
{
    public function InvitePanel (ctx :MsoyContext, invitee :MemberName)
    {
        _ctx = ctx;
        _invitee = invitee;

        PopUpManager.addPopUp(this, _ctx.getRootPanel(), true);
        PopUpManager.centerPopUp(this);
    }

    override protected function createChildren () :void
    {
        // set up the title
        var title :Label = new Label();
        title.text = _ctx.xlate("t.inviteGame", _invitee);

        // set up the actual configurator
        // TODO
        var gc :GameConfig = new FlashGameConfig();
        _configger = FlexGameConfigurator(gc.createConfigurator());
        _configger.init(_ctx);
        _configger.setGameConfig(gc);

        // set up OK and CANCEL buttons
        var okBtn :Button = new Button();
        okBtn.label = _ctx.xlate("b.ok");
        okBtn.addEventListener(MouseEvent.CLICK, okClicked);

        var cancelBtn :Button = new Button();
        cancelBtn.label = _ctx.xlate("b.cancel");
        cancelBtn.addEventListener(MouseEvent.CLICK, cancelClicked);

        // create a box to hold the buttons
        var butbox :HBox = new HBox();
        butbox.addChild(okBtn);
        butbox.addChild(cancelBtn);

        // add everything to the interface and pop it up
        addChild(title);
        addChild(_configger.getContainer());
        addChild(butbox);
    }

    protected function close () :void
    {
        PopUpManager.removePopUp(this);
    }

    protected function okClicked (event :MouseEvent) :void
    {
        var gc :GameConfig = _configger.getGameConfig();

        // HERE

        close();
    }

    protected function cancelClicked (event :MouseEvent) :void
    {
        close();
    }

    protected var _ctx :MsoyContext;

    protected var _invitee :MemberName;

    protected var _configger :FlexGameConfigurator;
}
}
