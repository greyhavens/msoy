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

import com.threerings.flex.CommandButton;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;


/**
 * A separate room editing panel, which lets the player edit furniture inside the room.
 */
public class RoomEditPanel extends FloatingPanel
{
    public function RoomEditPanel (
        ctx :WorldContext, anchor :DisplayObject, view :RoomView, wrapupFn :Function)
    {
        super(ctx, Msgs.EDITING.get("t.editor"));
        _anchor = anchor;
        _controller = new RoomEditController(ctx, this);
        _view = view;
        _wrapupFn = wrapupFn;

        styleName = "roomEditPanel";
        showCloseButton = true;
    }

    // from FloatingPanel
    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);

        var anchorRect :Rectangle = _anchor.getBounds(this);
// MDB: this always pops up over what I'm trying to edit
//         this.x = anchorRect.x;
//         this.y = anchorRect.y - this.height;
        this.x = 5;
        this.y = HeaderBar.HEIGHT + 5;

        _controller.init();
    }

    // from FloatingPanel
    override public function close () :void
    {
        super.close();

        _controller.deinit();
        _wrapupFn();
    }

    /** Returns current room layout. */
    public function get roomView () :RoomView
    {
        return _view;
    }

    /** Returns the edit controller. */
    public function get controller () :RoomEditController
    {
        return _controller;
    }

    /** Returns the advanced settings panel. */
    public function get advanced () :AdvancedSettingsPanel
    {
        return _advanced;
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
            function (action :String, button :Button, def :Object) :Function {
                return function (event :MouseEvent) :void {
                    resetOtherToggleButtons(button);
                    _controller.handleActionSelection(action, button, def);
                };
        }

        // now make me some buttons!

        var box :HBox = new HBox();
        box.styleName = "roomEditPanelContainer";
        box.percentWidth = 100;
        addChild(box);
        
        for each (var def :Object in BUTTON_DEFINITIONS)
        {
            // go through the list of definitions, and create buttons
            var button :Button = new Button();
            button.styleName = "roomEditButton" + (def.style as String);
            if (def.toggle as Boolean) {
                button.toggle = true;
                _toggleButtons.push(button);
            }
            button.toolTip = Msgs.EDITING.get(def.tip as String);
            button.enabled = (def.enabled == undefined || def.enabled as Boolean);
            button.addEventListener(
                MouseEvent.CLICK, makeEvent(def.action as String, button, def));

            // some special processing
            if (def.action == RoomEditController.ACTION_UNDO) {
                _undoButton = button;
            }

            box.addChild(button);
        }

        // make the label row

        var labelrow :HBox = new HBox();
        labelrow.styleName = "roomEditLabelContainer";
        labelrow.setStyle("verticalAlign", "middle");
        labelrow.percentWidth = 100;
        addChild(labelrow); 
        
        _label = new Label();
        _label.styleName = "roomEditLabel";
        labelrow.addChild(_label);

        var spacer :Spacer = new Spacer();
        spacer.percentWidth = 100;
        labelrow.addChild(spacer);

        var expand :CommandButton = new CommandButton();
        expand.styleName = "roomEditButtonExpand";
        expand.toggle = true;
        expand.width = expand.height = 9;
        expand.setCallback(toggleAdvanced);
        labelrow.addChild(expand);

        // make the advanced editing panel

        _advanced = new AdvancedSettingsPanel(_controller);
        addChild(_advanced);
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

    
    // Controller helper functions

    /** Called by the controller, to set the info label from the specified localized string. */
    public function setInfoLabel (def :Object) :void
    {
        _label.text =
            (def != null && def.info != null) ? Msgs.EDITING.get(def.info as String) : "";
    }

    /** Called by the controller, clears any focus rectangle displayed over the sprite. */
    public function clearFocus (sprite :MsoySprite) :void
    {
        if (sprite != null) {
            sprite.graphics.clear();
        }
    }
    
    /**
     * Called by the controller, to display a focus rectangle around the specified sprite.
     * The /action/ argument should be one of the RoomEditController.ACTION_* constants.
     */
    public function updateFocus (
        sprite :MsoySprite, action :String,
        drawStem :Boolean = false, highlightColor :uint = 0xffffff) :void
    {
        if (sprite == null) {
            return; // nothing to do
        }

        clearFocus(sprite);
        
        var g :Graphics = sprite.graphics;
        var w :Number = sprite.getActualWidth();
        var h :Number = sprite.getActualHeight();

        // compute location info for the stem from the current location to the floor
        if (drawStem) {
            // get sprite location in room and stage coordinates
            var roomLocation :MsoyLocation = sprite.getLocation();
            var stageLocation :Point =
                _view.localToGlobal(_view.layout.locationToPoint(roomLocation));
            var spriteLocation :Point = sprite.globalToLocal(stageLocation);

            // get root location by dropping the sprite y value, and converting back to screen
            var roomRoot :MsoyLocation = new MsoyLocation(roomLocation.x, 0, roomLocation.z, 0);
            var stageRoot :Point =
                _view.localToGlobal(_view.layout.locationToPoint(roomRoot));
            var spriteRoot :Point = sprite.globalToLocal(stageRoot); 
        }

        // draw the outline part of the border
        g.lineStyle(3, 0x000000, 0.5, true);
        g.drawRect(-2, -2, w + 3, h + 3);
        if (drawStem) {
            g.moveTo(spriteRoot.x, spriteRoot.y);
            g.lineTo(spriteLocation.x, spriteLocation.y);
        }

        // draw the white center of the border
        g.lineStyle(1, highlightColor, 1, true);
        g.drawRect(-2, -2, w + 3, h + 3);
        if (drawStem) {
            g.moveTo(spriteRoot.x, spriteRoot.y);
            g.lineTo(spriteLocation.x, spriteLocation.y);
        }

    }

    /** Called by the controller, sets undo button status based on the undo stack contents. */
    public function updateUndoButton (enabled :Boolean) :void
    {
        _undoButton.enabled = enabled;
    }
    
    /** Resets all toggle buttons except for the specified one. */
    protected function resetOtherToggleButtons (button :Button) :void
    {
        for each (var child :Button in _toggleButtons) {
            if (child.toggle && child != button) {
                child.selected = false;
            }
        }    
    }

    /** Toggles the visibility of advanced options sub-panel. */
    protected function toggleAdvanced (selected :Boolean) :void {
        if (_advanced != null) {
            _advanced.toggleVisibility();

            /*
            // todo: maybe this will be useful...
            if (selected) {
                // make sure we didn't roll out to overlap with the control bar
                var mybounds :Rectangle = this.getBounds(this.parent);
                var placebounds :Rectangle =
                    _ctx.getTopPanel().getPlaceContainer().getBounds(this.parent);
                
                if (mybounds.bottom > placebounds.bottom) {
                    this.y = placebounds.bottom - mybounds.height;
                }
            }
            */
        }
    }

    /** Constant button definitions. */
    protected static const BUTTON_DEFINITIONS :Array = [
        { style: "Room", toggle: false,
          action: RoomEditController.ACTION_ROOM,
          tip:  "i.room_button", info: "i.room_button_detail" },
        
        { style: "Move", toggle: true,
          action: RoomEditController.ACTION_MOVE,
          tip:  "i.move_button", info: "i.move_button_detail" },
        
        { style: "Scale", toggle: true,
          action: RoomEditController.ACTION_SCALE,
          tip:  "i.scale_button", info: "i.scale_button_detail" },
        
        { style: "Prefs", toggle: false,
          action: RoomEditController.ACTION_PREFS,
          tip:  "i.prefs_button", info: "i.prefs_button_detail" },
        
        { style: "Trash", toggle: true,
          action: RoomEditController.ACTION_DELETE,
          tip:  "i.delete_button", info: "i.delete_button_detail" },
        
        { style: "Undo", toggle: false, enabled: false,
          action: RoomEditController.ACTION_UNDO,
          tip: "i.undo_button", info: "i.undo_button_detail" },
        ];
    
    protected var _anchor :DisplayObject;
    protected var _controller :RoomEditController;
    protected var _advanced :AdvancedSettingsPanel;
    
    protected var _toggleButtons :Array = new Array();
    protected var _undoButton :Button;

    /** Info label. */
    protected var _label :Label;

    /** Focus sprite. */
    protected var _focus :Shape;

    /** Room we're editing. */
    protected var _view :RoomView;

    /** Callback function that gets called as the last step of shutdown. */
    protected var _wrapupFn :Function;
}
}
