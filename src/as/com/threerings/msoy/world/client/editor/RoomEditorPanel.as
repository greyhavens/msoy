//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.IEventDispatcher;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.containers.HBox;
import mx.controls.Button;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.FloatingPanel;


/**
 * A separate room editing panel, which lets the player edit furniture inside the room.
 */
public class RoomEditorPanel extends FloatingPanel
{
    public function RoomEditorPanel (ctx :WorldContext, controller :RoomEditorController)
    {
        super(ctx, Msgs.EDITING.get("t.editor"));
        _controller = controller;

        styleName = "roomEditPanel";
        showCloseButton = true;
    }

    // @Override from FloatingPanel
    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);

        this.x = 5;
        this.y = HeaderBar.HEIGHT + 5;
    }

    // @Override from FloatingPanel
    override public function close () :void
    {
        super.close();
        _controller.actionEditorClosed();
    }

    // from superclasses
    override protected function createChildren () :void
    {
        super.createChildren();
    }


    protected var _controller :RoomEditorController;
}
}
