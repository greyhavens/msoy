package com.threerings.msoy.client {

import flash.events.MouseEvent;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import com.threerings.flash.TextFieldUtil;
import com.threerings.flex.CommandButton;

import com.threerings.util.CommandEvent;

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
            nameLabel.text = Msgs.GENERAL.get("l.room_owner");
            _owner.addChild(nameLabel);
            nameLabel = new Label();
            nameLabel.styleName = "ownerName";
            nameLabel.text = owner;
            nameLabel.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                if (onClick != null) {
                    onClick();
                }
            });
            nameLabel.buttonMode = true;
            nameLabel.useHandCursor = true;
            nameLabel.mouseChildren = false;
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
        _owner.percentHeight = 100;
        _owner.setStyle("verticalAlign", "bottom");
        _owner.setStyle("horizontalGap", 0);
        addChild(_owner);

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
    }

    protected static const WHIRLED_LOGO_WIDTH :int = 124;

    protected var _ctx :WorldContext;

    protected var _controller :HeaderBarController;

    protected var _loc :Label;
    protected var _owner :HBox;
    protected var _embedLinkButton :CommandButton;
}
}
