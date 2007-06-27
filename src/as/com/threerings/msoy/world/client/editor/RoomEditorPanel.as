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
import mx.containers.TabNavigator;
import mx.controls.Button;
import mx.controls.HRule;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.data.FurniData;
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

    /** Updates object data displayed on the editing panel. */
    public function updateDisplay (data :FurniData) :void
    {
        _details.updateDisplay(data);
        _action.updateDisplay(data);
        _room.updateDisplay(data);
    }

    /** Updates the enabled status of the undo button (based on the size of the undo stack). */
    public function updateUndoStatus (enabled :Boolean) :void
    {
        _undoButton.enabled = enabled;
    }

    /** Updates the enabled status of the delete button (based on current selection). */
    public function updateDeleteStatus (enabled :Boolean) :void
    {
        if (_deleteButton != null) { // just in case this gets called during initialization...
            _deleteButton.enabled = enabled;
        }
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

        // now create tabs
        var c :CollapsingContainer = new CollapsingContainer(Msgs.EDITING.get("t.item_prefs"));
        c.setContents(_details = new DetailsPanel(_controller));
        addChild(c);

        var hr :HRule = new HRule();
        hr.percentWidth = 100;
        addChild(hr);

        c = new CollapsingContainer(Msgs.EDITING.get("t.item_action"));
        c.setContents(_action = new ActionPanel(_controller)); 
        addChild(c);

        hr = new HRule();
        hr.percentWidth = 100;
        addChild(hr);

        c = new CollapsingContainer(Msgs.EDITING.get("t.room_settings"));
        c.setContents(_room = new RoomPanel(_controller)); 
        addChild(c);
    }

    protected var _deleteButton :Button;
    protected var _undoButton :Button;
    protected var _details :DetailsPanel;
    protected var _action :ActionPanel;
    protected var _room :RoomPanel;
    protected var _controller :RoomEditorController;
}
}
