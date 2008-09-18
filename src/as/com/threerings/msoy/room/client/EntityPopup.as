//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.display.Shape;

import mx.containers.Canvas;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.WorldContext;

/**
 * A pop-up containing a user-code display object of some sort, to display
 * detailed information, or a configuration panel. Only one EntityPopup may be
 * displayed at a time, and only in response to a direct user click.
 */
public class EntityPopup extends FloatingPanel
{
    public function EntityPopup (
        ctx :WorldContext, entitySprite :MsoySprite, ctrl :RoomController,
        title :String, userPanel :DisplayObject, panelWidth :Number, panelHeight :Number,
        panelColor :uint = 0xFFFFFF, panelAlpha :Number = 1.0, doMasking :Boolean = true)
    {
        super(ctx,
            Msgs.GENERAL.get("t.entity_popup", Msgs.GENERAL.get(entitySprite.getDesc()), title));

        _ctrl = ctrl;
        _entitySprite = entitySprite;

        styleName = "entityPopup";
        showCloseButton = true;
        // TODO: a nice pop-up effect when this thing comes up. ZoomEffect.

        // don't let us get too big, we'd rather scroll
        maxWidth = 700;
        maxHeight = 500;

        // set up the canvas and the mask
        _canvas = new Canvas();
        _canvas.width = panelWidth;
        _canvas.height = panelHeight;
        if (doMasking) {
            var mask :Shape = new Shape();
            mask.graphics.beginFill(0xFFFFFF);
            mask.graphics.drawRect(0, 0, panelWidth, panelHeight);
            mask.graphics.endFill();
            _canvas.rawChildren.addChild(mask);
            _canvas.mask = mask;
        }

        setStyle("backgroundColor", panelColor);
        setStyle("backgroundAlpha", panelAlpha);

        // don't add the user panel yet. See note in open().
        _userPanel = userPanel;
    }

    public function getOwningEntity () :MsoySprite
    {
        return _entitySprite;
    }

    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, center :Boolean = true) :void
    {
        super.open(modal, parent, center);

        // Only add the user panel after all the flex components have created their content panes.
        // Otherwise, when the content pane is created a REMOVED_FROM_STAGE will be dispatched
        // to the userpanel, which is like the only reliable way to tell from inside the
        // entity when you've been removed. Fuck you flex! Why do wait to create the content pane
        // and then move all the children over, instead of just fucking creating it and adding
        // the children directly?
        _canvas.rawChildren.addChild(_userPanel);
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

    protected var _userPanel :DisplayObject;
}
}
