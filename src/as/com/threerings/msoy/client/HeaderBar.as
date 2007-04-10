package com.threerings.msoy.client {

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import com.threerings.flex.CommandButton;

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

    public function setLocationText (loc :String) :void
    {
        _loc.text = loc;
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = (_loc.textWidth + 5) > 124 ? _loc.textWidth + 5 : 124;
    }

    public function setCloseButtonVisible (visible :Boolean) :void
    {
        _closeBtn.includeInLayout = _closeBtn.visible = visible;
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
        _loc.width = 124;
        addChild(_loc);

        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        addChild(padding);

        var embedButtonBox :VBox = new VBox();
        embedButtonBox.styleName = "headerEmbedBox";
        embedButtonBox.percentHeight = 100;
        addChild(embedButtonBox);
        _embedLinkButton = new CommandButton(HeaderBarController.SHOW_EMBED_HTML);
        _embedLinkButton.styleName = "embedButton";
        _embedLinkButton.toolTip = Msgs.GENERAL.get("b.embed");
        embedButtonBox.addChild(_embedLinkButton);
        setEmbedLinkButtonVisible(false);

        var closeButtonBox :VBox = new VBox();
        closeButtonBox.styleName = "headerCloseBox";
        closeButtonBox.percentHeight = 100;
        addChild(closeButtonBox);
        _closeBtn = new CommandButton(HeaderBarController.CLOSE_CLIENT);
        _closeBtn.styleName = "closeButton";
        closeButtonBox.addChild(_closeBtn);
        setCloseButtonVisible(false);
    }

    protected var _ctx :WorldContext;

    protected var _controller :HeaderBarController;

    protected var _loc :Label;
    protected var _closeBtn :CommandButton;
    protected var _embedLinkButton :CommandButton;
}
}
