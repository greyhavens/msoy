//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.IEventDispatcher;
import flash.events.MouseEvent;
import flash.geom.Rectangle;

import mx.containers.HBox;
import mx.controls.Button;
import mx.controls.Label;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.ui.FloatingPanel;


/**
 * A separate room editing panel, which lets the player edit furniture inside the room.
 */
public class RoomEditPanel extends FloatingPanel
{
    public function RoomEditPanel (ctx :WorldContext, anchor :DisplayObject, view :RoomView)
    {
        super(ctx, Msgs.EDITING.get("t.editor"));
        _anchor = anchor;
        _controller = new RoomEditController(ctx, this);
        _view = view;

        styleName = "roomEditPanel";
        showCloseButton = true;
    }

    // from FloatingPanel
    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);

        var anchorRect :Rectangle = _anchor.getBounds(this);
        this.x = anchorRect.x;
        this.y = anchorRect.y - this.height;

        _controller.init();

    }

    // from FloatingPanel
    override public function close () :void
    {
        trace("*** CLOSE!");
        _controller.deinit();
        super.close();
    }

    /** Returns true if this panel is open on screen. */
    public function get isOpen () :Boolean
    {
        return (_parent != null);
    }

    /** Returns current room layout. */
    public function get roomView () :RoomView
    {
        return _view;
    }
    

    // from superclasses
    override protected function createChildren () :void
    {
        super.createChildren();

        // define a helper function that creates an event handler function. (this layer
        // of indirection is necessary because of actionscript's environment model; we want each
        // function instance to have its own local variable bindings, but those are stored
        // in the defining function's stack frame. so we want a fresh stack frame for each
        // function instance, and have to make one manually.)
        var makeEvent :Function =
            function (handler :Function, button :Button, def :Object) :Function {
                return function (event :MouseEvent) :void {
                    handler(button, def);
                };
        }

        // now make me some buttons!

        var defs :Array = [
            { style: "Room",  toggle: false,    tip: "i.room_button",
              fn: _controller.roomButtonClick,  info: "i.room_button_detail" },
            
            { style: "Move",  toggle: true,     tip:  "i.move_button",
              fn: _controller.moveButtonClick,  info: "i.move_button_detail" },
            
            { style: "Scale", toggle: true,     tip:  "i.scale_button",
              fn: _controller.scaleButtonClick, info: "i.scale_button_detail" },
            
            { style: "Trash", toggle: false, tip: "i.delete_button" },
            
            { style: "Undo",  toggle: false, tip: "i.undo_button" },
            ];

        var box :HBox = new HBox();
        box.styleName = "roomEditPanelContainer";
        addChild(box);
        
        for each (var def :Object in defs)
        {
            var button :Button = new Button();
            button.styleName = "roomEditButton" + (def.style as String);
            if (def.toggle as Boolean) {
                button.toggle = true;
                _toggleButtons.push(button);
            }
            button.toolTip = Msgs.EDITING.get(def.tip as String);
            if (def.fn != null) {
                button.addEventListener(
                    MouseEvent.CLICK, makeEvent(def.fn as Function, button, def));
            }
                                            
            box.addChild(button);
        }

        // make the label row

        var labelrow :HBox = new HBox();
        labelrow.styleName = "roomEditPanelContainer";
        addChild(labelrow); 
        
        _label = new Label();
        labelrow.addChild(_label);
        
    }

                                    

    /**
     * Returns true if avatar movement is enabled (most of the time), or false otherwise
     * (e.g. while moving a piece of furni).
     */
    public function get isMovementEnabled () :Boolean
    {
        // we want to capture movement during the modify phase (when the user is moving
        // or scaling objects in the room)
        return this.isOpen && 
            _controller.currentPhase != RoomEditController.PHASE_MODIFY;
    }

    /**
     * Called by the room contoller, on every frame when the mouse is over a sprite.
     */
    public function mouseOverSprite (sprite :MsoySprite) :void
    {
        if (this.isOpen) {
            _controller.mouseOverSprite(sprite);
        }
    }

    /**
     * Called by the room controller, on every frame, with new mouse position in stage units.
     */
    public function mouseMove (x :Number, y :Number) :void
    {
        if (this.isOpen) {
            _controller.mouseMove(x, y);
        }
    }

    /**
     * Called by the room controller to process mouse clicks. If the click landed on top of
     * a sprite, the /sprite/ variable will hold its reference.
     */
    public function mouseClick (sprite :MsoySprite, event :MouseEvent) :void
    {
        if (this.isOpen) {
            _controller.mouseClick(sprite, event);
        }
    }

    
    // Controller helper functions

    /** Called by the controller, to set the info label from the specified localized string. */
    public function setInfoLabel (def :Object) :void
    {
        _label.text =
            (def != null && def.info != null) ? Msgs.EDITING.get(def.info as String) : "";
    }

    /**
     * Called by the controller, to display a focus rectangle around the specified sprite.
     * When the /enabled/ parameter is true, the focus rectangle is displayed, otherwise removed.
     */
    public function updateFocus (sprite :MsoySprite, enabled :Boolean) :void
    {
        if (sprite == null) {
            return; // nothing to do
        }

        decorateSprite(sprite, enabled, true);
    }

    /** Called by the controller, resets all toggle buttons. */
    public function resetToggleButtons () :void
    {
        for each (var child :Button in _toggleButtons) {
            child.selected = false;
            }    
    }

    /**
     * Redraws sprite decoration, adding borders and/or stem.
     */
    public function decorateSprite (
        sprite :MsoySprite, drawBorder :Boolean, drawStem :Boolean) :void
    {
        var g :Graphics = sprite.graphics;
        g.clear();

        var w :Number = sprite.getActualWidth();
        var h :Number = sprite.getActualHeight();

        if (drawBorder) {
            g.lineStyle(3, 0x000000, 0.5, true);
            g.drawRect(-2, -2, w + 3, h + 3);
            g.lineStyle(1, 0xffffff, 1, true);
            g.drawRect(-2, -2, w + 3, h + 3);
        } 
    }
  
    protected var _anchor :DisplayObject;
    protected var _controller :RoomEditController;

    protected var _toggleButtons :Array = new Array();

    /** Info label. */
    protected var _label :Label;

    /** Focus sprite. */
    protected var _focus :Shape;

    /** Room we're editing. */
    protected var _view :RoomView;


}
}
