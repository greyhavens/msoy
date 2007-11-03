//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import flexlib.containers.ButtonScrollingCanvas;

import com.threerings.flash.TextFieldUtil;
import com.threerings.flex.CommandButton;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.chat.client.ChatTabBar;

import com.threerings.msoy.client.EmbedDialog;

public class HeaderBar extends HBox
{
    public static const HEIGHT :int = 20;

    public function HeaderBar (ctx :WorldContext, chatTabs :ChatTabBar) 
    {
        _ctx = ctx;
        _tabs = chatTabs;
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

    public function getChatTabs () :ChatTabBar
    {
        return _tabs;
    }

    public function setLocationName (loc :String) :void
    {
        _loc.text = loc;
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = Math.max(WHIRLED_LOGO_WIDTH, _loc.textWidth + TextFieldUtil.WIDTH_PAD);

        if (_tabsContainer.parent == this) {
            _tabs.setLocationName(loc);
        } else {
            _tabs.setLocationName(Msgs.CHAT.get("l.game_channel"));
        }
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
        _embedVisible = visible;
        _embedLinkButton.includeInLayout = _embedLinkButton.visible = visible;
    }

    public function miniChanged () :void
    {
        if (_ctx.getTopPanel().isMinimized()) {
            for each (var comp :UIComponent in _extras) {
                comp.includeInLayout = comp.visible = false;
            }
        } else {
            for each (comp in _extras) {
                if (comp == _embedLinkButton) {
                    comp.includeInLayout = comp.visible = _embedVisible;
                } else {
                    comp.includeInLayout = comp.visible = true;
                }
            }
        }
    }

    /**
     * Grab the tabs and the associated trimmings in the container that contains them, so they
     * can be moved somewhere else.
     */
    public function removeTabsContainer () :UIComponent
    {
        if (_tabsContainer.parent == this) {
            removeChild(_tabsContainer);
        }
        _loc.visible = _loc.includeInLayout = true;
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = Math.max(WHIRLED_LOGO_WIDTH, _loc.textWidth + TextFieldUtil.WIDTH_PAD);
        return _tabsContainer;
    }

    public function replaceTabsContainer () :void
    {
        if (_tabsContainer.parent != null) {
            _tabsContainer.parent.removeChild(_tabsContainer);
        }
        _tabs.setLocationName(_loc.text);
        if (_loc.parent == this) {
            _loc.visible = _loc.includeInLayout = false;
        }
        addChildAt(_tabsContainer, 1);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _loc = new Label();
        _loc.styleName = "locationName";
        _loc.width = WHIRLED_LOGO_WIDTH;
        _loc.visible = _loc.includeInLayout = false;
        addChild(_loc);

        _tabsContainer = new HBox();
        _tabsContainer.setStyle("horizontalGap", 0);
        _tabsContainer.setStyle("borderStyle", "none");
        _tabsContainer.setStyle("paddingTop", 0);
        _tabsContainer.setStyle("paddingBottom", 0);
        (_tabsContainer as HBox).horizontalScrollPolicy = "off"; 
        addChild(_tabsContainer);

        var channelBtn :CommandButton = new CommandButton();
        channelBtn.setCommand(MsoyController.POP_CHANNEL_MENU, channelBtn);
        channelBtn.toolTip = Msgs.GENERAL.get("i.channel");
        channelBtn.styleName = "headerBarButtonChannel";
        _tabsContainer.addChild(channelBtn);

//        var canvas :ButtonScrollingCanvas = new ButtonScrollingCanvas();
//        canvas.setStyle("borderStyle", "none");
//        canvas.setStyle("backgroundAlpha", 0);
//        canvas.setStyle("paddingTop", 0);
//        canvas.setStyle("paddingBottom", 0);
//        canvas.startScrollingEvent = MouseEvent.MOUSE_DOWN;
//        canvas.stopScrollingEvent = MouseEvent.MOUSE_UP;
//        canvas.scrollSpeed = 100;
//        canvas.addChild(_tabs);
//        _tabsContainer.addChild(canvas);
        _tabsContainer.addChild(_tabs);

        _owner = new HBox();
        _owner.styleName = "ownerNameBox";
        _owner.percentHeight = 100;
        addChild(_owner);
        _extras.push(_owner);

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
        _extras.push(_embedLinkButton);

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

    protected var _loc :Label;
    protected var _owner :HBox;
    protected var _embedLinkButton :Label;

    protected var _tabs :ChatTabBar;

    /** Bits that get removed when in minimized view */
    protected var _extras :Array = [];

    protected var _embedVisible :Boolean;

    protected var _tabsContainer :UIComponent;
}
}
