//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.controls.Button;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.Keyboard;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.client.RoomView;


/**
 * Controller for the room editing panel.
 */
public class RoomEditorController
{
    // The controller handles the different editing actions that can be applied to objects
    // in a room. Actions can come from three sources: button presses on the editor popup,
    // hotspot presses on small UI displayed on the sprite, or button presses in the room.
    // Controller processes inputs based on the combination of the current state
    // (are we editing the furni, and if so, what kind of action), and updates the scene
    // appropriately.

    public function RoomEditorController (ctx :WorldContext, ctrl :RoomController)
    {
        _ctx = ctx;
        _ctrl = ctrl;

        _edit = new FurniEditor(this);
        _hover = new FurniHighlight(this);
    }

    public function init (view :RoomView) :void
    {
        _view = view;

        _view.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        _view.addEventListener(MouseEvent.MOUSE_UP, mouseUp);
    }

    public function deinit () :void
    {
        _view.removeEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        _view.removeEventListener(MouseEvent.MOUSE_UP, mouseUp);

        _view = null;
    }

    public function get roomView () :RoomView
    {
        return _view;
    }

    
    public function isEditing () :Boolean
    {
        return _view != null && _panel != null && _panel.isOpen;
    }

    /**
     * Called by the room controller, pops up the UI and starts editing.
     */
    public function startEditing (wrapupFn :Function) :void
    {
        if (_view == null) {
            Log.getLog(this).warning("Cannot edit a null room view!");
        }
        
        //trace("*** startEditing");
        
        _panel = new RoomEditorPanel(_ctx, this);
        _wrapupFn = wrapupFn;

        _view.setEditing(true);
        _edit.start();
        _hover.start();

        _panel.open();
      
    }

    /**
     * Called by the room controller, causes the editor window to close and all editing
     * functionality to get wrapped up.
     */
    public function endEditing () :void
    {
        //trace("*** endEditing");

        _panel.close();
        
        // note: the rest of cleanup will happen in actionEditorClosed
    }

    /** Called by the room controller, to specify whether the undo stack is empty. */
    public function updateUndoStatus (isEmpty :Boolean) :void
    {
        // FIXME ROBERT: dim the button if empty
    }

    /** Called by the room controller, to query whether the user should be allowed to move
     *  around the scene. */
    public function isMovementEnabled() :Boolean
    {
        return isIdle();
    }


    
    // ACTIONS. These functions are called when the user clicks a panel button.

    /** Called by the editor panel, cleans up current editing session. */
    public function actionEditorClosed () :void
    {
        //trace("*** actionEditorClosed");
        
        if (_panel != null && _panel.isOpen) {
            Log.getLog(this).warning("Room editor failed to close!");
        }
        
        _edit.end();
        _hover.end();
        _view.setEditing(false);
        
        _wrapupFn();
        _panel = null;
    }



    // INPUT GATHERERS AND DISPATCHERS
    
    /** Called by the room controller, when the user rolls over or out of a valid sprite. */
    public function mouseOverSprite (sprite :MsoySprite) :void
    {
        var sprite :MsoySprite = isIdle() ? sprite : null;
        if (_hover.target != sprite &&
            (_edit.target != null ? _edit.target != sprite : true))
        {
            // either the player is hovering over a new sprite, or switching from an old
            // target to nothing at all. in either case, update!
            _hover.target = sprite;
        }
    }

    /** Called by the room controller, when the user clicks on a valid sprite. */
    public function mouseClickOnSprite (sprite :MsoySprite, event :MouseEvent) :void
    {
        if (isIdle()) {
            //trace("*** SELECTING SPRITE: " + sprite);
            _hover.target = null;
            _edit.target = sprite;
        }
    }

    /** Called by the room controller with mouse movement updates (in stage coordinates). */
    public function mouseMove (sx :Number, sy :Number) :void
    {
        if (isIdle()) {
            _edit.mouseMove(sx, sy);
        }
    }

    /** Receives mouse down events from the room view. */
    protected function mouseDown (event :MouseEvent) :void
    {
        //trace("*** MOUSE DOWN");
    }

    /** Receives mouse up events from the room view. */
    protected function mouseUp (event :MouseEvent) :void
    {
        //trace("*** MOUSE UP");
    }
    


    // STATE-SPECIFIC HANDLERS

    protected function isIdle () :Boolean
    {
        return isEditing() && _state == STATE_IDLE;
    }
    

    // STATE_* constants correspond to the general editing states of this controller

    /** Idle state, in which no modification is being performed, but the current target is
     *  highlighted, and a new target can be selected. */
    protected static const STATE_IDLE   :int = 0;
    /** State in which all inputs are interpreted as movement on the floor plane. */
    protected static const STATE_FLOOR  :int = 1;
    /** State in which all inputs are interpreted as movement on the y-axis. */
    protected static const STATE_HEIGHT :int = 2;
    /** State in which all inputs are interpreted as sprite scaling. */
    protected static const STATE_SCALE  :int = 3;

    /** Default no-op handler. */
    protected function none (... args) :void { /* no op */ } 
        
    protected var _ctx :WorldContext;
    protected var _ctrl :RoomController;
    protected var _view :RoomView;
    protected var _edit :FurniEditor;
    protected var _hover :FurniHighlight;
    protected var _panel :RoomEditorPanel;
    protected var _wrapupFn :Function;   // will be called when ending editing

    /** Current editing state, as one of the STATE_* constants. */
    protected var _state :int = STATE_IDLE;
    
    
}
}
