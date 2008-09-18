//
// $Id$

package com.threerings.msoy.room.client.editor {

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Text;
import mx.core.Container;
import mx.core.ScrollPolicy;

import com.threerings.util.Log;

import com.threerings.presents.client.ResultAdapter;

import com.threerings.flex.CommandButton;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.RoomObjectController;
import com.threerings.msoy.room.client.updates.FurniUpdateAction;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.RoomObject;

/**
 * This controller handles in-world door editing. The player picks a door to edit, then travels
 * the whirled in search of a target room. Once target is selected, the player will be returned
 * to the door location, and the door will be set to point to the target scene.
 *
 * Note: this object is a singleton, because it needs to exist independently of the different
 * room views that get created when player moves between scenes.
 */
public class DoorTargetEditController
{
    /**
     * Singleton constructor. Do not instantiate the singleton directly - use the static
     * function start() instead.
     */
    public function DoorTargetEditController ()
    {
        // this would be simpler if Actionscript supported non-public constructors. why doesn't it?
        if (_this != null) {
            throw new Error("DoorTargetEditController must not be instantiated directly.");
        }
    }

    /** Returns true if a door is currently being edited. */
    public static function get editing () :Boolean
    {
        return (_this._doorScene != 0);
    }

    /**
     * Returns true if a door is currently being edited, and we're in the middle of
     * committing a new door target selection.
     */
    protected static function get committing () :Boolean
    {
        return editing && (_this._destinationScene != 0);
    }

    /**
     * Start editing a door. Displays the target editor window, which waits for the player to click
     * on a 'submit' button to specify target location.
     */
    public static function start (doorData :FurniData, ctx :WorldContext) :void
    {
        if (editing) {
            _this.deinit();
        } else {
            _this.init(doorData, ctx);
        }
    }

    /**
     * Initializes all internal data structures.
     */
    protected function init (doorData :FurniData, ctx :WorldContext) :void
    {
        _ctx = ctx;
        _container = ctx.getTopPanel().getPlaceContainer();
        _ui = makeUI();
        _ui.open();
        _ui.x = 5;
        _ui.y = HeaderBar.HEIGHT + 5;

        _doorScene = _ctx.getSceneDirector().getScene().getId();
        _doorId = doorData.itemId;

        _destinationScene = 0;
        _destinationLoc = null;
        _destinationName = null;
    }

    /**
     * Shuts down any editing data structures.
     */
    protected function deinit (doorData :FurniData = null) :void
    {
        // if we got update info...
        if (doorData != null) {
            // ...create a furni update based on the door data, and send it to the server.
            var ctrl :RoomObjectController =
                _ctx.getLocationDirector().getPlaceController() as RoomObjectController;

            var newdata :FurniData = doorData.clone() as FurniData;
            // note: the destinationName may have colons in it, so we split with care in  FurniData
            newdata.actionData = _destinationScene + ":" + roundCoord(_destinationLoc.x) + ":" +
                roundCoord(_destinationLoc.y) + ":" +  roundCoord(_destinationLoc.z) + ":" +
                _destinationLoc.orient + ":" + _destinationName;

            ctrl.applyUpdate(new FurniUpdateAction(_ctx, doorData, newdata));
        }

        // now clean up
        _doorId = _doorScene = _destinationScene = 0;
        _destinationLoc = null;
        _destinationName = null;

        _ui.close();
        _ui = null;
        _container = null;
        _ctx = null;
    }

    /** Used to round locations to nearest hundredths to avoid giant actionData strings. */
    protected function roundCoord (value :Number) :Number
    {
        return Math.round(value / .01) * .01;
    }

    /** Creates the UI. */
    protected function makeUI () :FloatingPanel
    {
        var panel :FloatingPanel = new FloatingPanel(_ctx, Msgs.EDITING.get("t.edit_door"));
        panel.showCloseButton = true;

        var label :Text = new Text();
        label.text = Msgs.EDITING.get("l.edit_door_label");
        label.width = 400; // force the damned text to wrap
        panel.addChild(label);

        var elts :HBox = new HBox();
        elts.setStyle("right", 2);
        elts.setStyle("left", 2);
        elts.setStyle("top", 20);
        elts.setStyle("bottom", 2);
        elts.verticalScrollPolicy = ScrollPolicy.OFF;
        panel.addChild(elts);

        elts.addChild(new CommandButton(Msgs.EDITING.get("b.edit_door_ok"), select));
        elts.addChild(new CommandButton(Msgs.EDITING.get("b.buy_room"), purchase));

        return panel;
    }

    /**
     * Called when the player hits the 'select' button.
     */
    protected function select () :void
    {
        var sd :SceneDirector = _ctx.getSceneDirector();
        if (sd != null) {
            // the door should point to where we are right now
            setDoor(sd.getScene().getId());
        }
    }

    /**
     * Called when the player hits the 'purchase' button.
     */
    protected function purchase () :void
    {
        var roomObj :RoomObject = (_ctx.getLocationDirector().getPlaceObject() as RoomObject);
        roomObj.roomService.purchaseRoom(_ctx.getClient(), new ResultAdapter (
            function (cause :String) :void { // failure handler
                Log.getLog(this).info("Room purchase failure: " + cause);
                _ctx.displayFeedback(null, cause);
            },
            function (result :Object) :void { // success handler
                if (result != null) {
                    var newSceneId :int = int(Number(result));
                    // Log.getLog(this).info("Room purchase success, id = " + newSceneId);
                    setDoor(newSceneId);
                }
            }));
    }

    /**
     * Given the target scene Id, this function starts the chain of events that will
     * set the door target and move the player back to the room where the door was.
     */
    protected function setDoor (targetSceneId :int) :void
    {
        // the sequence of events started by this function is as follows:
        // 1. we remember the current scene as the new target, and issue a request
        //    to transfer the player back to the scene with the door.
        // 2. once we've traveled there, we set the door target to point to the new location.
        //
        // this baroque order of operations reflects a usage pattern in our code,
        // which requires that a scene be loaded up before it can be edited.

        var sd :SceneDirector = _ctx.getSceneDirector();
        if (sd == null) {
            Log.getLog(this).warning("Room purchase failure: scene director not initialized.");
            return;
        }

        var scene :MsoyScene = sd.getScene() as MsoyScene;

        // remember the target
        _destinationScene = targetSceneId;
        _destinationLoc = _ctx.getSpotSceneDirector().getIntendedLocation() as MsoyLocation;
        _destinationName = _ctx.getSceneDirector().getScene().getName();

        // are we already in the room with the door?
        if (scene.getId() == _doorScene) {
            // get ready to rock!
            finalizeCommit(scene);

        } else {
            // move the player back to the room with the door
            sd.moveTo(_doorScene);
            // the rest will be triggered via updateLocation(), once we get there...
        }
    }

    /**
     * Called when the player enters a new room. If we're committing a door, and we
     * just traveled to the room where the door was placed, finish up editing.
     */
    public static function updateLocation () :void
    {
        // we only care about this if we're actually in the process of setting a target door
        if (committing) {
            var scene :MsoyScene = _this._ctx.getSceneDirector().getScene() as MsoyScene;

            // if we're editing, and we traversed back to the original door location,
            // update the door and end editing.
            if (scene.getId() == _this._doorScene) {
                _this.finalizeCommit(scene);
            }
        }
    }

    /**
     * Called after we've returned to the room with the door, will set the door's target.
     */
    protected function finalizeCommit (scene :MsoyScene) :void
    {
        // find the door furni
        var furnis :Array = scene.getFurni();
        for each (var data :FurniData in furnis) {
            // check to make sure the object still exists there, and is still a door.
            // todo: we probably want some kind of a lock here.
            if (data.itemId == _doorId && data.actionType == FurniData.ACTION_PORTAL) {
                deinit(data);
                return;
            }
        }

        // the door went away? just cancel.
        deinit();
    }

    /** Singleton constant. */
    protected static var _this :DoorTargetEditController = new DoorTargetEditController();

    /** Scene ID where the door resides. */
    protected var _doorScene :int = 0;

    /** Item ID of the door. */
    protected var _doorId :int = 0;

    /** Scene ID of the door destination. */
    protected var _destinationScene :int = 0;

    /** The location at which to arrive in our destination. */
    protected var _destinationLoc :MsoyLocation;

    /** The name of the destination scene. */
    protected var _destinationName :String;

    /** Flex container for the scene. */
    protected var _container :Container;

    /** World context, what else? */
    protected var _ctx :WorldContext;

    /** Canvas that contains the editing UI. */
    protected var _ui :FloatingPanel;
}
}


