//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.Keyboard;

import com.threerings.flex.CommandButton;
import com.threerings.io.TypedArray;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;
import com.threerings.presents.client.ResultWrapper;
import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;
import com.threerings.util.HashMap;
import com.threerings.util.Log;
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
import com.threerings.msoy.world.data.SceneAttrsUpdate;


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

        // listen for mouse down
        _view.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);

        // clear out the names cache, and ping the server
        _names = new HashMap();
        queryServerForNames(this.scene.getFurni());

        // make the fake entrance
        _entranceSprite = new EntranceSprite(scene.getEntrance());
        _entranceSprite.setEditing(true);
        _view.addOtherSprite(_entranceSprite);
        var id :ItemIdent = _entranceSprite.getFurniData().getItemIdent();
        _names.put(id, { label: Msgs.EDITING.get("l.entrance"), data: id });
    }

    /**
     * Called by the room controller to close the editor.
     */
    public function endEditing () :void
    {
        // tell the panel to close
        _panel.close();

        // note: this function does *not* get called when the player closes the editor by
        // clicking on the close button. all significant editor cleanup should happen in 
        // actionEditorClosed()
    }

    /**
     * Receives a scene update from the controller, and refreshes the edited target appropriately.
     */
    public function processUpdate (update :SceneUpdate) :void
    {
        if (! isEditing()) {
            // don't care about updates if we're not actually editing.
            return;
        }
        
        if (update is SceneAttrsUpdate) {
            var up :SceneAttrsUpdate = update as SceneAttrsUpdate;
            // update sprite data
            _entranceSprite.getFurniData().loc.set(up.entrance);
            _entranceSprite.update(_entranceSprite.getFurniData());
            refreshTarget();
            return;
        }
        
        if (update is ModifyFurniUpdate) {
            var mod :ModifyFurniUpdate = update as ModifyFurniUpdate;

            // special case: most of the time the player will just move furnis around, which
            // results in an update that removes and re-adds the same object.
            // if that's what's going on, just do a simple refresh.
            if (mod.furniRemoved != null && mod.furniRemoved.length == 1 &&
                mod.furniAdded != null && mod.furniAdded.length == 1) {

                var added :FurniData = mod.furniAdded[0] as FurniData;
                var removed :FurniData = mod.furniRemoved[0] as FurniData;
                if (added.getItemIdent().equals(removed.getItemIdent())) {
                    refreshTarget();
                    return;
                }
            }

            // this is a different kind of an update. refresh the name cache appropriately.
            queryServerForNames(mod.furniAdded);
            updateNameDisplay();
            
            // finally, if the target furni just got removed, we should lose focus.
            if (mod.furniRemoved != null && _edit.target != null) {
                var targetIdent :ItemIdent = _edit.target.getFurniData().getItemIdent();
                var targetRemoved :Boolean = mod.furniRemoved.some(
                    function (furni :FurniData, ... rest) :Boolean {
                        return furni.getItemIdent().equals(targetIdent);
                    });
                if (targetRemoved) {
                    setTarget(null, null);
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
     * Called by the targetting system, applies a furni change to the scene.
     */
    public function updateFurni (toRemove :FurniData, toAdd :FurniData) :void
    {
        if (toAdd is EntranceFurniData) {
            // entrace is actually a fake furni, and entrance data lives in the scene model
            var newscene :MsoyScene = scene.clone() as MsoyScene;
            var newmodel :MsoySceneModel = newscene.getSceneModel() as MsoySceneModel;
            newmodel.entrance = toAdd.loc;
            updateScene(scene, newscene);
        } else {
            // it's a genuine furni update - apply it 
            _view.getRoomController().applyUpdate(new FurniUpdateAction(_ctx, toRemove, toAdd));
        }
        updateUndoStatus(true);
    }

    /**
     * Called by the panel, applies a property change to the scene.
     */
    public function updateScene (oldScene :MsoyScene, newScene :MsoyScene) :void
    {
        _view.getRoomController().applyUpdate(new SceneUpdateAction(_ctx, oldScene, newScene));
        updateUndoStatus(true);
    }

    /**
     * Called by the panel when the name list selection changed, either programmatically
     * or due to user interaction - causes the specified item to be selected as the new target.
     * Note: this searches through the list of sprites; use setTarget() directly if possible.
     */
    public function findAndSetTarget (ident :ItemIdent) :void
    {
        // if the player selected the special "new item" option, pop up the inventory panel
        if (ident == NEW_ITEM_SELECTION) {
            CommandEvent.dispatch(_panel.parent, MsoyController.VIEW_MY_FURNITURE);
            setTarget(null, null);
            return;
        }

        // is this our special entrance sprite? if so, it's not in the room contents list.
        if (ident.equals(EntranceFurniData.ITEM_IDENT)) {
            setTarget(_entranceSprite, null);
            return;
        }
        
        // it's a bona fide selection. if the new target is different, let's select it
        if (_edit.target == null || ! _edit.target.getFurniData().getItemIdent().equals(ident)) {
            var sprites :Array = _view.getFurniSprites().values();
            // unfortunately, we have to search through all sprites to find the one we want
            var index :int = ArrayUtil.indexIf(sprites, function (sprite :FurniSprite) :Boolean {
                    return sprite.getFurniData().getItemIdent().equals(ident);
                });
            setTarget(index == -1 ? null : sprites[index], null);
        }
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

        // stop listening for mouse down
        _view.removeEventListener(MouseEvent.MOUSE_DOWN, mouseDown);

        _entranceSprite.setEditing(false);
        _view.removeOtherSprite(_entranceSprite);
        _entranceSprite = null;

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
        if (_hover.target != sprite && (_edit.target != null ? _edit.target != sprite : true)) {
            // either the player is hovering over a new sprite, or switching from an old
            // target to nothing at all. in either case, update!
            _hover.target = sprite as FurniSprite;
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
     * Handles mouse presses, starts editing furniture.
     */
    protected function mouseDown (event :MouseEvent) :void
    {
        var hit :MsoySprite =
            (_view.getRoomController().getHitSprite(event.stageX, event.stageY, true) as MsoySprite);
        if (hit is FurniSprite) {
            if (_edit.isIdle()) {
                _hover.target = null;
                setTarget(hit as FurniSprite, event);
            }
        }
    }

    /**
     * Helper function, returns an array of ItemIdents of pieces of furniture from the specified
     * /furnis/ array, whose names are not stored in the cache.
     */
    protected function findNamelessFurnis (furnis :Array) :TypedArray /* of ItemIdent */
    {
        var idents :TypedArray = TypedArray.create(ItemIdent);
        for each (var data :FurniData in furnis) {
            var ident :ItemIdent = data.getItemIdent();
            if (! _names.containsKey(ident)) {          // only query for new items
                if (data.itemType != Item.NOT_A_TYPE) { // skip freebie doors and other fake items
                    idents.push(ident);
                }
            }
        }
        return idents;
    }
        
    /**
     * Given a list of furnis, retrieves names of furnis we don't yet know about.
     */
    protected function queryServerForNames (furnis :Array /* of FurniData */) :void
    {
        if (furnis == null) {
            return; // nothing to do
        }
        
        // find which furni names we're missing
        var idents :TypedArray = findNamelessFurnis(furnis);

        if (idents.length == 0) {
            return; // no names are missing - we're done!
        }
        
        // now ask the server for ids
        var svc :ItemService = _ctx.getClient().requireService(ItemService) as ItemService;
        svc.getItemNames(
            _ctx.getClient(), idents, new ResultWrapper(function (cause :String) :void {
                // do nothing
                Log.getLog(RoomEditorController).warning(
                    "Unable to get item names [cause=" + cause + "].");
            }, function (names :Array /* of String */) :void {
                // we got an array of names! put them all in the cache and update the list.
                for (var i :int = 0; i < idents.length; i++) {
                    _names.put(idents[i], { label: names[i], data: idents[i] });
                }
                updateNameDisplay();
            }));
    }

    /**
     * When the user clicks on a new item, updates its displayed name.
     */
    protected function selectTargetName () :void
    {
        // if there's no furni selected, we have nothing to do
        if (_edit.target == null) {
            _panel.selectInNameList(null);
            return;
        }

        // pull out selected furni
        var targetData :FurniData = _edit.target.getFurniData();
        var ident :ItemIdent = targetData.getItemIdent();
        _panel.updateDisplay(targetData);

        // if this is a special furni, deal with it in a special way
        if (EntranceFurniData.ITEM_IDENT.equals(ident)) {
            _panel.selectInNameList(_names.get(ident));
            return;
        }

        if (ident.type == Item.NOT_A_TYPE) {
            // this must be one of the "freebie" doors - since this isn't an actual Item,
            // it has no name.
            _panel.selectInNameList(null);
            return;
        }

        // update display name
        if (_names.containsKey(ident)) {
            _panel.selectInNameList(_names.get(ident));
        } else {
            _panel.selectInNameList(null);
            Log.getLog(RoomEditorController).debug("Furni name not found! [id=" + ident + "].");
        }
    }

    /** Called when the list of objects in the room had changed, it updates the panel. */
    protected function updateNameDisplay () :void
    {
        var idents :Array = this.scene.getFurni().map(
            function(furni :FurniData, i :*, a :*) :ItemIdent {
                return furni.getItemIdent();
            });
        var defs :Array = _names.values().filter(function (def :Object, i :*, a :*) :Boolean {
            return ArrayUtil.contains(idents, def.data);
        });

        defs.push(_names.get(EntranceFurniData.ITEM_IDENT));
        defs.sortOn("label", Array.CASEINSENSITIVE);

        // if we're running in a browser, add the "new item" selection
        if (! _ctx.getWorldClient().isEmbedded()) {
            defs.unshift({ label: Msgs.EDITING.get("l.new_furni"), data: NEW_ITEM_SELECTION });
        }
        
        _panel.updateNameList(defs);
        selectTargetName();
    }

    /** Sets the currently edited target to the specified sprite. */
    protected function setTarget (targetSprite :FurniSprite, event :MouseEvent) :void
    {
        _edit.target = targetSprite;
        if (event != null) {
            _edit.defaultHotspot.implicitStartAction(event);
        }
        targetSpriteUpdated();
        selectTargetName();
    }

    /** Forces the target sprite to be re-read from the room. */
    protected function refreshTarget () :void
    {
        // if the player selected the singleton entrance sprite, our work is done
        if (_edit.target is EntranceSprite) {
            setTarget(_entranceSprite, null);
            return;
        }

        // otherwise, try to find the right sprite in the room, and refresh the target from that
        if (_edit.target != null) {
            var sprites :HashMap = _view.getFurniSprites();
            setTarget(sprites.get(_edit.target.getFurniData().id) as FurniSprite, null);
        } else {
            targetSpriteUpdated();
        }
    }

    /**
     * Mapping from ItemIdents to combo box entries that contain both names and ItemIdents.
     * This cache is updated once when the editor is opened, and then following each furni update. 
     */
    protected var _names :HashMap = new HashMap();

    /** Special ItemIdent instance for the "new item" option in name selection box. */
    protected static const NEW_ITEM_SELECTION :ItemIdent = new ItemIdent();
    
    protected var _ctx :WorldContext;
    protected var _view :RoomView;
    protected var _edit :FurniEditor;
    protected var _hover :FurniHighlight;
    protected var _panel :RoomEditorPanel;
    protected var _wrapupFn :Function;   // will be called when ending editing
    
    protected var _entranceSprite :EntranceSprite;
}
}
