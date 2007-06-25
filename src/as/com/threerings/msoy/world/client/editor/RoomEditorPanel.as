//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.IEventDispatcher;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.containers.HBox;
import mx.controls.Button;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
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

        this.x = TopPanel.DECORATIVE_MARGIN_HEIGHT;
        this.y = HeaderBar.HEIGHT + TopPanel.DECORATIVE_MARGIN_HEIGHT;
    }

    // @Override from FloatingPanel
    override public function close () :void
    {
        super.close();
        _controller.actionEditorClosed();
    }

    public function updateUndoStatus (enabled :Boolean) :void
    {
        _undoButton.enabled = enabled;
    }
    
    public function updateDeleteStatus (enabled :Boolean) :void
    {
        _deleteButton.enabled = enabled;
    }
    
    // from superclasses
    override protected function createChildren () :void
    {
        super.createChildren();

        var makeListener :Function = function (thunk :Function) :Function {
            return function (event :Event) :void { thunk(); };
        };
        
        // container for buttons
        var box :HBox = new HBox();
        box.styleName = "roomEditPanelContainer";
        box.percentWidth = 100;
        addChild(box);

        // create a button for each definition
        _deleteButton = new Button();
        _deleteButton.styleName = "roomEditButtonTrash";
        _deleteButton.toolTip = Msgs.EDITING.get("i.delete_button");
        _deleteButton.enabled = false;
        _deleteButton.addEventListener(MouseEvent.CLICK, makeListener(_controller.actionDelete));
        box.addChild(_deleteButton);
        
        _undoButton = new Button();
        _undoButton.styleName = "roomEditButtonUndo";
        _undoButton.toolTip = Msgs.EDITING.get("i.undo_button");
        _undoButton.enabled = false;
        _undoButton.addEventListener(MouseEvent.CLICK, makeListener(_controller.actionUndo));
        box.addChild(_undoButton);
        
    }

    protected var _deleteButton :Button;
    protected var _undoButton :Button;
    protected var _controller :RoomEditorController;
}
}
