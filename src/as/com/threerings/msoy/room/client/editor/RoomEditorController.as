//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.events.MouseEvent;

import com.threerings.io.TypedArray;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.presents.client.ResultAdapter;

import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;
import com.threerings.util.Log;
import com.threerings.util.StringUtil;

import com.threerings.msoy.client.MemberService;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.room.client.FurniSprite;
import com.threerings.msoy.room.client.MsoySprite;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.updates.FurniUpdateAction;
import com.threerings.msoy.room.client.updates.SceneUpdateAction;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate_Add;
import com.threerings.msoy.room.data.FurniUpdate_Change;
import com.threerings.msoy.room.data.FurniUpdate_Remove;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.SceneAttrsUpdate;

/**
 * Controller for the room editing panel. It starts up two different types of UI: one is
 * a regular Flex window with buttons like "delete" and "undo", and the other is a furni editor,
 * displayed as a border around the targetted furni with grabbable hotspots to manipulate it.
 */
public class RoomEditorController
{
    public function RoomEditorController (ctx :WorldContext, view :RoomObjectView)
    {
        _ctx = ctx;
        _view = view;

        _edit = new FurniEditor(this);
        _hover = new FurniHighlight(this);
    }

    public function get roomView () :RoomObjectView
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
        return _view != null && _panel != null && _panel.isOpen();
    }

    /**
     * Initializes all editing UIs and starts editing the room.
     */
    public function startEditing (wrapupFn :Function) :void
    {
        if (_view == null) {
            log.warning("Cannot edit a null room view!");
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
        _entranceSprite = new EntranceSprite(_ctx, scene.getEntrance());
        _entranceSprite.setEditing(true);
        _view.addOtherSprite(_entranceSprite);
        var id :ItemIdent = _entranceSprite.getFurniData().getItemIdent();
        _names.put(id, { label: Msgs.EDITING.get("l.entrance"), data: id });

        _panel.setDecor((scene.getSceneModel() as MsoySceneModel).decor);

        // hide advanced ui
        actionAdvancedEditing(false);
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
            var up :SceneAttrsUpdate = (update as SceneAttrsUpdate);
            // update sprite data
            _entranceSprite.getFurniData().loc.set(up.entrance);
            _entranceSprite.update(_entranceSprite.getFurniData());
            _panel.setDecor(up.decor);

            refreshTarget();

        } else if (update is FurniUpdate_Add) {
            queryServerForNames([ (update as FurniUpdate_Add).data ]);
            updateNameDisplay();

        } else if (update is FurniUpdate_Change) {
            refreshTarget();

        } else if (update is FurniUpdate_Remove) {
            // if the target furni just got removed, we should lose focus.
            if (_edit.target != null && _edit.target.getFurniData().getItemIdent().equals(
                    (update as FurniUpdate_Remove).data.getItemIdent())) {
                setTarget(null, null);
            }
            updateNameDisplay();
        }
    }

    /** Called by the controller and other functions, to update the panel's undo button. */
    public function updateUndoStatus (undoAvailable :Boolean) :void
    {
        _panel.updateUndoStatus(undoAvailable);
    }

    /** Called by the targetting system when a target is being selected. */
    public function updateTargetSelected () :void
    {
        _panel.updateTargetSelected(_edit.target);
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
            _view.getRoomObjectController().applyUpdate(
                new FurniUpdateAction(_ctx, toRemove, toAdd));
        }
        updateUndoStatus(true);
    }

    /**
     * Called by the panel, applies a property change to the scene.
     */
    public function updateScene (oldScene :MsoyScene, newScene :MsoyScene) :void
    {
        _view.getRoomObjectController().applyUpdate(
            new SceneUpdateAction(_ctx, oldScene, newScene));
        updateUndoStatus(true);
    }

    /**
     * Called by the panel when the name list selection changed, either programmatically
     * or due to user interaction - causes the specified item to be selected as the new target.
     * Note: this searches through the list of sprites; use setTarget() directly if possible.
     */
    public function findAndSetTarget (ident :ItemIdent) :void
    {
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
        updateUndoStatus(_view.getRoomObjectController().undoLastUpdate());
    }

    /** Performs a Delete action on the currently selected target. */
    public function actionDelete () :void
    {
        // delete the currently selected item
        if (_edit.target != null) {
            updateFurni(_edit.target.getFurniData(), null);
        }
    }

    /** Adjusts furni size from a panel button action. */
    public function actionAdjustScale (multiplierX :Number, multiplierY :Number) :void
    {
        withFurniUpdate(function () :void {
            if (multiplierX != 1 || multiplierY != 1) {
                var f :FurniData = _edit.target.getFurniData().clone() as FurniData;
                _edit.updateTargetScale(multiplierX * f.scaleX, multiplierY * f.scaleY);
            }
        });
    }

    /** Adjusts furni size from a panel button action. */
    public function actionAdjustRotation (
        rotationDelta :Number, snap :Boolean = false, snapIncrement :Number = 0) :void
    {
        withFurniUpdate(function () :void {
            if (rotationDelta != 0) {
                // rotate the furni
                var f :FurniData = _edit.target.getFurniData();
                var newrotation :Number = f.rotation + rotationDelta;

                // only do this calculation if we're not already snapped
                if (snap && (f.rotation % snapIncrement != 0)) {
                    // use delta to snap to the specified increment
                    var snapfn :Function = (rotationDelta > 0) ? Math.ceil : Math.floor;
                    newrotation = snapIncrement * snapfn(f.rotation / snapIncrement);
                }

                _edit.updateTargetRotation(newrotation);
            }
        });
    }

    /** Adjusts furni location from a panel button action. */
    public function actionAdjustYPosition (yDelta :Number) :void
    {
        withFurniUpdate(function () :void {
            if (yDelta != 0) {
                var f :FurniData = _edit.target.getFurniData().clone() as FurniData;
                _edit.updateTargetLocation(
                    new MsoyLocation(f.loc.x, f.loc.y + yDelta, f.loc.z));
            }
        });
    }

    /** Resets the edited furni to the base location, or size, or both. */
    public function actionResetTarget (
        resetLocation :Boolean, resetSize :Boolean, resetRotation :Boolean) :void
    {
        withFurniUpdate(function () :void {
            if (resetLocation) {
                _edit.updateTargetLocation(new MsoyLocation(0.5, 0.5, 0.5));
            }
            if (resetSize) {
                _edit.updateTargetScale(1.0, 1.0);
            }
            if (resetRotation) {
                _edit.updateTargetRotation(0);
            }
        });
    }

    /** Tells the room controller to start editing the target as a door. */
    public function actionTargetDoor () :void
    {
        if (_edit.target == null || ! _edit.target.isActionModifiable()) {
            return;
        }

        var data :FurniData = _edit.target.getFurniData();

        // make the furni's type to a portal, and save on the server
        withFurniUpdate(function () :void {
            data.actionType = FurniData.ACTION_PORTAL;
            data.actionData = "" + scene.getId() + ":" + scene.getName();
        });

        // now open up the door creation wizard. note: we're not wrapping this
        // in a furni update, because the room controller code will do that for us.
        _view.getRoomObjectController().handleEditDoor(data);
    }

    /** Starts editing the URL. */
    public function actionTargetLink (url :String, tip :String) :void
    {
        var actionData :String = url;
        tip = StringUtil.trim(tip);
        if (!StringUtil.isBlank(tip)) {
            actionData += "||" + tip;
        }
        setTargetAction(FurniData.ACTION_URL, actionData);
    }

    /** Makes the target into a regular furni. */
    public function actionTargetClear () :void
    {
        setTargetAction(FurniData.ACTION_NONE, null);
    }

    /**
     * Cleans up editing actions and closes editing UIs. This function is called automatically
     * when the main editing UI is being closed (whether because the user clicked the close
     * button, or because the room controller cancelled the editing session).
     */
    public function actionEditorClosed () :void
    {
        if (_panel != null && _panel.isOpen()) {
            log.warning("Room editor failed to close!");
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

    /** Tells the room controller the user updated advanced editing preferences. */
    public function actionAdvancedEditing (advanced :Boolean) :void
    {
        _edit.setAdvancedMode(advanced);
        _panel.displayAdvancedPanels(advanced);
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
        _panel.updateDisplay(_edit.target);
    }

    public function makeHome () :void
    {
        _panel.setHomeButtonEnabled(false);

        var model :MsoySceneModel = scene.getSceneModel() as MsoySceneModel;
        var svc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        svc.setHomeSceneId(_ctx.getClient(), model.ownerType, model.ownerId, model.sceneId, 
            new ReportingListener(_ctx, MsoyCodes.EDITING_MSGS, null,
                (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) ? "m.group_home_room_changed"
                                                                     : "m.home_room_changed"));
    }

    /**
     * Handles mouse presses, starts editing furniture.
     */
    protected function mouseDown (event :MouseEvent) :void
    {
        var hit :MsoySprite = (_view.getRoomController().getHitSprite(
                               event.stageX, event.stageY, true) as MsoySprite);
        if (hit is FurniSprite) {
            if (_edit.isIdle()) {
                _hover.target = null;
                setTarget(hit as FurniSprite, event);
            }
        }
    }

    /**
     * Wraps the call to /thunk/ in a check for furni existence, and triggers an update
     * to be sent to the server if /thunk/ modified the furni in any way.
     */
    protected function withFurniUpdate (thunk :Function) :void
    {
        if (_edit.target == null) {
            return;
        }

        var original :FurniData = _edit.target.getFurniData().clone() as FurniData;
        thunk();

        var current :FurniData = _edit.target.getFurniData();
        if (! original.equivalent(current)) {
            updateFurni(original, current);
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
            _ctx.getClient(), idents, new ResultAdapter(function (cause :String) :void {
                // do nothing
                log.warning("Unable to get item names [cause=" + cause + "].");
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
        _panel.updateDisplay(_edit.target);

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
            log.debug("Furni name not found! [id=" + ident + "].");
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

    /** Sets the currently edited target's action (if applicable). */
    protected function setTargetAction (actionType :int, actionData :String) :void
    {
        withFurniUpdate(function () :void {
            if (_edit.target == null || ! _edit.target.isActionModifiable()) {
                return;
            }

            var data :FurniData = _edit.target.getFurniData();
            data.actionType = actionType;
            data.actionData = actionData;
        });
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

    protected var _ctx :WorldContext;
    protected var _view :RoomObjectView;
    protected var _edit :FurniEditor;
    protected var _hover :FurniHighlight;
    protected var _panel :RoomEditorPanel;
    protected var _wrapupFn :Function;   // will be called when ending editing

    /** Mapping from ItemIdents to combo box entries that contain both names and ItemIdents.  This
     * cache is updated once when the editor is opened, and then following each furni update. */
    protected var _names :HashMap = new HashMap();

    protected var _entranceSprite :EntranceSprite;

    private const log :Log = Log.getLog(this);
}
}
