package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Button;
import mx.controls.Text;
import mx.core.Container;
import mx.core.ScrollPolicy;

import com.threerings.io.TypedArray;
import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.updates.FurniUpdateAction;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneUpdate;



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
     * Start editing a door. Displays the target editor window, which waits for the player
     * to click on a 'submit' button to specify target location.
     */
    public static function start (doorData :FurniData, ctx :WorldContext) :void
    {
        if (editing) {
            _this.deinit();
        }

        _this.init(doorData, ctx);
    }
    
    /**
     * Initializes all internal data structures.
     */
    protected function init (doorData :FurniData, ctx :WorldContext) :void
    {
        _ctx = ctx;
        _container = ctx.getTopPanel().getPlaceContainer();
        _ui = makeUI();

        _doorScene = _ctx.getSceneDirector().getScene().getId();
        _doorId = doorData.itemId;
        _destinationScene = 0;
                
        _container.addChild(_ui);
    }

    /**
     * Shuts down any editing data structures. 
     */
    protected function deinit (doorData :FurniData = null, view :RoomView = null) :void
    {
        // if we got update info, send it to the server
        if (doorData != null && view != null) {
            updateDoor(doorData, view);
        }

        // now clean up
        _container.removeChild(_ui);
        
        _doorId = _doorScene = _destinationScene = 0;
        
        _ui = null;
        _container = null;
        _ctx = null;
    }

    /**
     * Creates a furni update based on provided door data, and sends it to the server.
     */
    protected function updateDoor (doorData :FurniData, view :RoomView) :void
    {
        var newdata :FurniData = doorData.clone() as FurniData;
        newdata.actionData = String(_destinationScene);

        view.getRoomController().applyUpdate(
            new FurniUpdateAction(_ctx, doorData, newdata));
    }

    /** Creates the UI. */
    protected function makeUI () :Container
    {
        var panel :Container = new Canvas();
        panel.styleName = "doorEditPanel";
        panel.horizontalScrollPolicy = ScrollPolicy.OFF;
        panel.verticalScrollPolicy = ScrollPolicy.OFF;
        panel.width = 120;  // do i want these absolute?
        panel.height = 100;
        panel.x = panel.y = 0;

        // cancel button in the upper right corner
        var bcancel :Button = new Button();
        bcancel.setStyle("icon", CANCEL_ICON);
        bcancel.addEventListener(MouseEvent.CLICK, cancel);
        bcancel.styleName = "doorEditButton";
        bcancel.width = bcancel.height = 13;
        bcancel.setStyle("right", 2);
        bcancel.setStyle("top", 2);
        panel.addChild(bcancel);

        var elts :VBox = new VBox();
        elts.setStyle("right", 2);
        elts.setStyle("left", 2);
        elts.setStyle("top", 20);
        elts.setStyle("bottom", 2);
        elts.verticalScrollPolicy = ScrollPolicy.OFF;
        panel.addChild(elts);

        var label :Text = new Text();
        label.text = Msgs.GENERAL.get("l.edit_door_label");
        label.percentWidth = 100;
        label.percentHeight = 100;
        elts.addChild(label);

        var bcommit :Button = new Button();
        bcommit.label = Msgs.GENERAL.get("b.edit_door_ok");
        bcommit.addEventListener(MouseEvent.CLICK, select);
        bcommit.styleName = "doorEditButton";
        bcommit.percentWidth = 100;
        elts.addChild(bcommit);

        return panel;
    }

    /**
     * Called when the player hits the 'close' button. Cancels editing.
     */
    public function cancel (event :MouseEvent) :void
    {
        deinit();
    }

    /**
     * Called when the player hits the 'select' button. Starts the chain of events that will
     * set the door target and move the player back to the room where the door was.
     */
    protected function select (event :MouseEvent) :void
    {
        // the sequence of events started by this function is as follows:
        // 1. we remember the current scene as the new target, and issue a request
        //    to transfer the player back to the scene with the door.
        // 2. once there, we set the door target to point to the new location.
        //
        // this baroque order of operations reflects a usage pattern in our code,
        // which requires that a scene be loaded up before it can be edited.
        
        var sd :SceneDirector = _ctx.getSceneDirector();
        if (sd != null) {
            
            // remember where we are
            _destinationScene = sd.getScene().getId();
            
            // ask the player to move back to the old room
            sd.moveTo(_doorScene);
        }
    }

    /**
     * Processes notifications when the player enters a new room.
     */
    public static function setRoomView (view :RoomView) :void
    {
        // we only care about this if we're actually in the process of setting a target door
        if (committing) {
            var scene :MsoyScene = _this._ctx.getSceneDirector().getScene() as MsoyScene;

            // if we're editing, and we traversed back to the original door location,
            // update the door and end editing.
            if (scene.getId() == _this._doorScene) {
                _this.finalizeCommit(scene, view);
            }
        }
    }

    /**
     * Called after we've returned to the room with the door, will set the door's target.
     */
    protected function finalizeCommit (scene :MsoyScene, view :RoomView) :void
    {
        // find the door furni
        var furnis :Array = scene.getFurni();
        for each (var data :FurniData in furnis) {
            // check to make sure the object still exists there, and is still a door.
            // todo: we probably want some kind of a lock here.   
            if (data.itemId == _doorId && data.actionType == FurniData.ACTION_PORTAL) {
                deinit(data, view);
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
    
    /** Flex container for the scene. */       
    protected var _container :Container;

    /** World context, what else? */
    protected var _ctx :WorldContext;

    /** Canvas that contains the editing UI. */
    protected var _ui :Container;

    // Button media. 
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/close.png")]
    protected static const CANCEL_ICON :Class;
}
}
    

