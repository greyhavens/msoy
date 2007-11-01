//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import com.threerings.flash.TextFieldUtil;
import com.threerings.flex.CommandButton;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.client.EmbedDialog;

public class HeaderBar extends HBox
{
    public static const HEIGHT :int = 20;

    public function HeaderBar (ctx :WorldContext) 
    {
        _ctx = ctx;
        styleName = "headerBar";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        percentWidth = 100;
        height = HEIGHT;

        _controller = new HeaderBarController(ctx, this);
    }

    public function getController () :HeaderBarController
    {
        return _controller;
    }

    public function getLocationText () :String
    {
        return _tabs.getLocationName();
    }

    public function setLocationText (loc :String) :void
    {
        _tabs.setLocationName(loc);
    }

    public function addTab (tabName :String) :void
    {
        _tabs.addChatTab(tabName);
    }

    public function setOwnerLink (owner :String, onClick :Function = null) :void 
    {
        while (_owner.numChildren > 0) {
            _owner.removeChildAt(0);
        }
        if (owner != "") {
            var nameLabel :Label = new Label();
            nameLabel.styleName = "ownerName";
            nameLabel.text = Msgs.GENERAL.get("m.room_owner", owner);
            nameLabel.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                if (onClick != null) {
                    onClick();
                }
            });
            var embedded :Boolean = _ctx.getWorldClient().isEmbedded();
            nameLabel.buttonMode = !embedded;
            nameLabel.useHandCursor = !embedded;
            nameLabel.mouseChildren = embedded;
            _owner.addChild(nameLabel);
        }
    }

    public function setEmbedLinkButtonVisible (visible :Boolean) :void
    {
        _embedLinkButton.includeInLayout = _embedLinkButton.visible = visible;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var channelBtn :CommandButton = new CommandButton();
        channelBtn.setCommand(MsoyController.POP_CHANNEL_MENU, channelBtn);
        channelBtn.toolTip = Msgs.GENERAL.get("i.channel");
        channelBtn.styleName = "headerBarButtonChannel";
        addChild(channelBtn);

        _tabs = new HeaderChatTabBar();
        addChild(_tabs);

        _owner = new HBox();
        _owner.styleName = "ownerNameBox";
        _owner.percentHeight = 100;
        addChild(_owner);

        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        addChild(padding);

        var controlBox :HBox = new HBox();
        controlBox.styleName = "headerEmbedBox";
        controlBox.percentHeight = 100;
        addChild(controlBox);

        _embedLinkButton = new Label();
        _embedLinkButton.styleName = "embedButton";
        _embedLinkButton.text = Msgs.GENERAL.get("l.share");
        _embedLinkButton.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
            new EmbedDialog(_ctx);
        });
        _embedLinkButton.buttonMode = true;
        _embedLinkButton.useHandCursor = true;
        _embedLinkButton.mouseChildren = false;
        controlBox.addChild(_embedLinkButton);
        setEmbedLinkButtonVisible(false);

        var closeBox :VBox = new VBox();
        closeBox.styleName = "headerCloseBox";
        controlBox.addChild(closeBox);
        var closeBtn :CommandButton = new CommandButton();
        closeBtn.setCommand(MsoyController.CLOSE_PLACE_VIEW);
        closeBtn.styleName = "closeButton";
        closeBox.addChild(closeBtn);
    }

    protected static const WHIRLED_LOGO_WIDTH :int = 124;

    protected var _ctx :WorldContext;

    protected var _controller :HeaderBarController;

    protected var _owner :HBox;
    protected var _embedLinkButton :Label;

    protected var _tabs :HeaderChatTabBar;
}
}
