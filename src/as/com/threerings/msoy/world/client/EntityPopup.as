//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Shape;
import flash.display.Sprite;

import mx.containers.Canvas;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * A pop-up containing a user-code display object of some sort, to display
 * detailed information, or a configuration panel. Only one EntityPopup may be
 * displayed at a time, and only in response to a direct user click.
 */
public class EntityPopup extends FloatingPanel
{
    public function EntityPopup (
        ctx :WorldContext, entitySprite :MsoySprite, ctrl :RoomController,
        title :String, userPanel :DisplayObject, panelWidth :Number, panelHeight :Number)
    {
        super(ctx,
            Msgs.GENERAL.get("t.entity_popup", Msgs.GENERAL.get(entitySprite.getDesc()), title));

        _ctrl = ctrl;
        _entitySprite = entitySprite;

        styleName = "entityPopup";
        showCloseButton = true;
        // TODO: a nice pop-up effect when this thing comes up. ZoomEffect.

        // don't let us get too big, we'd rather scroll dahling
        maxWidth = 400;
        maxHeight = 300;

        // set up the canvas and the mask
        _canvas = new Canvas();
        _canvas.width = panelWidth;
        _canvas.height = panelHeight;
        var mask :Shape = new Shape();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, panelWidth, panelHeight);
        mask.graphics.endFill();
        _canvas.rawChildren.addChild(mask);
        _canvas.mask = mask;

        // we need to put the userpanel one-level down, otherwise flex stuff will
        // try to examine it and freak out
        var holder :Sprite = new Sprite();
        holder.addChild(userPanel);
        _canvas.rawChildren.addChild(holder);
    }

    public function getOwningEntity () :MsoySprite
    {
        return _entitySprite;
    }

    override public function close () :void
    {
        super.close();
        _ctrl.entityPopupClosed();
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        addChild(_canvas);
    }

    /** We use this to control the size allocated for the displayed content. */
    protected var _canvas :Canvas;

    protected var _ctrl :RoomController;

    /** The sprite that owns this. */
    protected var _entitySprite :MsoySprite;
}
}
