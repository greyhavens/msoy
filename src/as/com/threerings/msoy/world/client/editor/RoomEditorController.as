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
import com.threerings.util.HashMap;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.updates.FurniUpdateAction;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.FurniData;


/**
 * Controller for the room editing panel. It starts up two different types of UI: one is
 * a regular Flex window with buttons like "delete" and "undo", and the other is a furni editor,
 * displayed as a border around the targetted furni with grabbable hotspots to manipulate it.
 */
public class RoomEditorController
{
    public function RoomEditorController (ctx :WorldContext, view :RoomView)
    {
        _ctx = ctx;
        _view = view;

        _edit = new FurniEditor(this);
        _hover = new FurniHighlight(this);
    }

    public function get roomView () :RoomView
    {
        return _view;
    }

    /**
     * Returns true if the room is currently being edited.
     */
    public function isEditing () :Boolean
    {
        return _view != null && _panel != null && _panel.isOpen;
    }

    /**
     * Initializes all editing UIs and starts editing the room.
     */
    public function startEditing (wrapupFn :Function) :void
    {
        if (_view == null) {
            Log.getLog(this).warning("Cannot edit a null room view!");
        }
        
        _panel = new RoomEditorPanel(_ctx, this);
        _wrapupFn = wrapupFn;

        _view.setEditing(true);
        _edit.start();
        _hover.start();

        _panel.open();
      
    }

    /**
     * Called by the room controller, cancels any current editing functions.
     */
    public function endEditing () :void
    {
        _panel.close();
        
        // note: the rest of cleanup will happen in actionEditorClosed
    }

    /**
     * Receives a scene update from the controller, and refreshes the edited target appropriately.
     */
    public function processUpdate (update :SceneUpdate) :void
    {
        if (update is ModifyFurniUpdate) {
            var mod :ModifyFurniUpdate = update as ModifyFurniUpdate;

            // check if the currently selected furni was modified
            var targetAdded :Boolean, targetRemoved :Boolean;
            var targetIdent :ItemIdent = _edit.target.getFurniData().getItemIdent();
            
            mod.furniRemoved.some(function (furni :FurniData, ... rest) :Boolean {
                    if (furni.getItemIdent().equals(targetIdent)) {
                        targetRemoved = true;
                        return true;
                    }
                    return false;    
                });
            
            mod.furniAdded.some(function (furni :FurniData, ... rest) :Boolean {
                    if (furni.getItemIdent().equals(targetIdent)) {
                        targetAdded = true;
                        return true;
                    }
                    return false;
                });

            if (targetRemoved) {
                if (targetAdded) {
                    // if the target furni was removed and then added back in, it means
                    // it got modified. reread it!
                    refreshTarget();
                } else {
                    // the target furni got removed - we should lose the focus as well.
                    setTarget(null);
                }
            }
        }
    }

    /**
     * Called by the room controller, to specify whether the undo stack is empty.
     */
    public function updateUndoStatus (isEmpty :Boolean) :void
    {
        // FIXME ROBERT: dim the button if empty
    }

    /**
     * Called by the room editor, requests that an update be applied changing a furni
     * from an old version to a new version.
     */
    public function updateFurni (toRemove :FurniData, toAdd :FurniData) :void
    {
        _view.getRoomController().applyUpdate(new FurniUpdateAction(_ctx, toRemove, toAdd));
        // _panel.updateUndoButton(true);
    }
    
    /**
     * Called by the room controller, to query whether the user should be allowed to move
     * around the scene.
     */
    public function isMovementEnabled() :Boolean
    {
        return isEditing() && _edit.isIdle();
    }

    /**
     * Cleans up editing actions and closes editing UIs. This function is called automatically
     * when the main editing UI is being closed (whether because the user clicked the close
     * button, or because the room controller cancelled the editing session).
     */
    public function actionEditorClosed () :void
    {
        if (_panel != null && _panel.isOpen) {
            Log.getLog(this).warning("Room editor failed to close!");
        }
        
        _edit.end();
        _hover.end();
        _view.setEditing(false);
        
        _wrapupFn();
        _panel = null;
    }


    // Functions for highlighting targets and displaying the furni editing UI
    
    /** Called by the room controller, when the user rolls over or out of a valid sprite. */
    public function mouseOverSprite (sprite :MsoySprite) :void
    {
        var sprite :MsoySprite = _edit.isIdle() ? sprite : null;
        if (_hover.target != sprite &&
            (_edit.target != null ? _edit.target != sprite : true))
        {
            // either the player is hovering over a new sprite, or switching from an old
            // target to nothing at all. in either case, update!
            _hover.target = sprite as FurniSprite;
        }
    }

    /** Called by the room controller, when the user clicks on a valid sprite. */
    public function mouseClickOnSprite (sprite :MsoySprite, event :MouseEvent) :void
    {
        if (_edit.isIdle()) {
            _hover.target = null;
            setTarget(sprite as FurniSprite);
        }
    }

    /**
     * Called after target sprite modification, it will update all UIs to update their parameters.
     */
    public function targetSpriteUpdated () :void
    {
        _edit.updateDisplay();
        // todo: update flex panel here
    }

    /** Sets the currently edited target to the specified sprite. */
    protected function setTarget (targetSprite :FurniSprite) :void
    {
        _edit.target = targetSprite;
        targetSpriteUpdated();
    }

    /** Forces the target sprite to be re-read from the room. */
    protected function refreshTarget () :void
    {
        if (_edit.target != null) {
            var sprites :HashMap = _view.getFurniSprites();
            setTarget(sprites.get(_edit.target.getFurniData().id) as FurniSprite);
        }
    }
       
    protected var _ctx :WorldContext;
    protected var _view :RoomView;
    protected var _edit :FurniEditor;
    protected var _hover :FurniHighlight;
    protected var _panel :RoomEditorPanel;
    protected var _wrapupFn :Function;   // will be called when ending editing

    
}
}
