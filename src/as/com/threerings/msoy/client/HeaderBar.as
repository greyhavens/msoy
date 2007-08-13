//
// $Id$

package com.threerings.msoy.client {

import flash.events.MouseEvent;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import com.threerings.flash.TextFieldUtil;

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
        return _loc.text;
    }

    public function setLocationText (loc :String) :void
    {
        _loc.text = loc;
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = Math.max(WHIRLED_LOGO_WIDTH, _loc.textWidth + TextFieldUtil.WIDTH_PAD);
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

        _loc = new Label();
        _loc.styleName = "locationName";
        _loc.width = WHIRLED_LOGO_WIDTH;
        addChild(_loc);

        _owner = new HBox();
        _owner.styleName = "ownerNameBox";
        _owner.percentHeight = 100;
        addChild(_owner);

        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        addChild(padding);

        var embedButtonBox :VBox = new VBox();
        embedButtonBox.styleName = "headerEmbedBox";
        embedButtonBox.percentHeight = 100;
        addChild(embedButtonBox);
        _embedLinkButton = new Label();
        _embedLinkButton.styleName = "embedButton";
        _embedLinkButton.text = Msgs.GENERAL.get("l.share");
        _embedLinkButton.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
            new EmbedDialog(_ctx);
        });
        _embedLinkButton.buttonMode = true;
        _embedLinkButton.useHandCursor = true;
        _embedLinkButton.mouseChildren = false;
        embedButtonBox.addChild(_embedLinkButton);
        setEmbedLinkButtonVisible(false);
    }

    protected static const WHIRLED_LOGO_WIDTH :int = 124;

    protected var _ctx :WorldContext;

    protected var _controller :HeaderBarController;

    protected var _loc :Label;
    protected var _owner :HBox;
    protected var _embedLinkButton :Label;
}
}
