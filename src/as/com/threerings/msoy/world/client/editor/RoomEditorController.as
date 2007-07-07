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

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.presents.client.ResultWrapper;
import com.threerings.util.HashMap;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.updates.FurniUpdateAction;
import com.threerings.msoy.world.client.updates.SceneUpdateAction;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
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

    public function get ctx () :WorldContext
    {
        return _ctx;
    }

    public function get scene () :MsoyScene
    {
        return _ctx.getSceneDirector().getScene() as MsoyScene;
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
        // we only care about this if a target is selected...
        if (update is ModifyFurniUpdate && _edit.target != null) {
            var mod :ModifyFurniUpdate = update as ModifyFurniUpdate;

            // check if the currently selected furni was modified
            var targetAdded :Boolean, targetRemoved :Boolean;
            var targetIdent :ItemIdent = _edit.target.getFurniData().getItemIdent();

            if (mod.furniRemoved != null) {
                targetRemoved = mod.furniRemoved.some(
                    function (furni :FurniData, ... rest) :Boolean {
                        return furni.getItemIdent().equals(targetIdent);
                    });
            }

            if (mod.furniAdded != null) {
                targetAdded = mod.furniAdded.some(
                    function (furni :FurniData, ... rest) :Boolean {
                        return furni.getItemIdent().equals(targetIdent);
                    });
            }

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

    /** Called by the controller and other functions, to update the panel's undo button. */
    public function updateUndoStatus (undoAvailable :Boolean) :void
    {
        _panel.updateUndoStatus(undoAvailable);
    }

    /** Called by the targetting system, to update the panel's delete button. */
    public function updateDeleteStatus (deleteAvailable :Boolean) :void
    {
        _panel.updateDeleteStatus(deleteAvailable);
    }

    /**
     * Requests that the specified furni update be applied to the scene.
     */
    public function updateFurni (toRemove :FurniData, toAdd :FurniData) :void
    {
        _view.getRoomController().applyUpdate(new FurniUpdateAction(_ctx, toRemove, toAdd));
        updateUndoStatus(true);
    }

    /**
     * Requests that the specified scene update be applied to the scene.
     */
    public function updateScene (oldScene :MsoyScene, newScene :MsoyScene) :void
    {
        _view.getRoomController().applyUpdate(new SceneUpdateAction(_ctx, oldScene, newScene));
        updateUndoStatus(true);
    }

    /**
     * Called by the room controller, to query whether the user should be allowed to move
     * around the scene.
     */
    public function isMovementEnabled() :Boolean
    {
        return isEditing() && _edit.isIdle();
    }

    /** Performs an Undo action, if possible. */
    public function actionUndo () :void
    {
        // undo the last action, and set undo button's enabled state appropriately
        updateUndoStatus(_view.getRoomController().undoLastUpdate());
    }

    /** Performs a Delete action on the currently selected target. */
    public function actionDelete () :void
    {
        // delete the currently selected item
        if (_edit.target != null) {
            updateFurni(_edit.target.getFurniData(), null);
        }
    }

    /** Tells the room controller to start editing the specified door. */
    public function actionEditDoor (data :FurniData) :void
    {
        _view.getRoomController().handleEditDoor(data);
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
        _panel.updateDisplay(_edit.target != null ? _edit.target.getFurniData() : null);
    }

    /**
     * When the user clicks on a new item, updates its displayed name.
     */
    protected function updateTargetName () :void
    {
        if (_edit.target == null) {
            _lastIdent = null;
            // no target selected
            _panel.updateName(null);
            return;
        }

        var furniData :FurniData = _edit.target.getFurniData();
        _panel.updateDisplay(furniData);
        var ident :ItemIdent = furniData.getItemIdent();
        _lastIdent = ident;

        if (ident.type == Item.NOT_A_TYPE) {
            // this must be one of the "freebie" doors - since this isn't an actual Item, we can't
            // pull its name from the database. oh well.
            _panel.updateName(Msgs.EDITING.get("t.editing_no_name"));
            return;
        }

        // update the name..
        var name :String = _names.get(ident) as String;
        _panel.updateName(name);
        if (name != null) {
            return;
        }

        // We don't know the name, and need to look it up.
        // Put a fake name in there for this ident, so we don't fire off a
        // second request should this get called again before the response arrives
        _names.put(ident, "...");

        // perform a database query to get the item's name
        var svc :ItemService = _ctx.getClient().requireService(ItemService) as ItemService;
        svc.getItemName(_ctx.getClient(), ident, new ResultWrapper(function (cause :String) :void {
            Log.getLog(RoomEditorController).warning(
                "Unable to get name of item [cause=" + cause + "].");
            // and do nothing
        }, function (name :String) :void {
            _names.put(ident, name); // cache update
            if (ident.equals(_lastIdent)) {
                _panel.updateName(name);
            }
        }));
    }

    /** Sets the currently edited target to the specified sprite. */
    protected function setTarget (targetSprite :FurniSprite) :void
    {
        _edit.target = targetSprite;
        targetSpriteUpdated();
        updateTargetName();
    }

    /** Forces the target sprite to be re-read from the room. */
    protected function refreshTarget () :void
    {
        if (_edit.target != null) {
            var sprites :HashMap = _view.getFurniSprites();
            setTarget(sprites.get(_edit.target.getFurniData().id) as FurniSprite);
        }
    }

    /**
     * Cache that maps from ItemIdents to item names; it saves server round-trips when the
     * user edits different items in the room. Please note: item names are not updated after the
     * first lookup, and may become stale.
     */
    protected var _names :HashMap = new HashMap();

    /** The ItemIdent of the currently selected furni. */
    protected var _lastIdent :ItemIdent;

    protected var _ctx :WorldContext;
    protected var _view :RoomView;
    protected var _edit :FurniEditor;
    protected var _hover :FurniHighlight;
    protected var _panel :RoomEditorPanel;
    protected var _wrapupFn :Function;   // will be called when ending editing
}
}
