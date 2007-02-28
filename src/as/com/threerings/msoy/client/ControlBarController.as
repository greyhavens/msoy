//
// $Id$

package com.threerings.msoy.client {

import flash.events.IEventDispatcher;
import mx.controls.Button;

import com.threerings.presents.client.ClientEvent;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.whirled.data.Scene;
import com.threerings.crowd.client.LocationAdapter;
import com.threerings.util.CommandEvent;
import com.threerings.util.Controller;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.MsoyScene;


/**
 * Controller for actions from the ControlBar UI.
 *
 * The controller takes care of actions specific to the control bar (e.g. volume popup).
 * Also, it keeps track of the client's movement through the scenes, and supports
 * movement "back" through the stack of previously visited scenes, akin to the back button
 * in a web browser.
 */
public class ControlBarController extends Controller
{
    public static const log :Log = Log.getLog(ControlBarController);

    /** Command to display a volume slider. */
    public static const POP_VOLUME :String = "handlePopVolume";

    /** Command to move back to the previous location. */
    public static const MOVE_BACK :String = "handleMoveBack";

    /** Command to open room editing mode. */
    public static const EDIT_SCENE :String = "handleEditScene";
    
    /** Create the controller. */
    public function ControlBarController (
        ctx :WorldContext, topPanel :TopPanel, controlBar :ControlBar)
    {
        _ctx = ctx;
        _topPanel = topPanel;
        _controlBar = controlBar;
        _backstack = new Array ();
        
        setControlledPanel(controlBar);
        _location = new LocationAdapter(null, this.locationChanged, null);
        _logon = new ClientAdapter(null, this.logonChanged, null, this.logonChanged);
    }

    /**
     * Registers or unregisters for updates on the client's location
     */
    public function registerForSessionObservations (registration :Boolean) :void
    {
        if (registration) {
            _ctx.getClient().addClientObserver(_logon);
            _ctx.getLocationDirector().addLocationObserver(_location);
        } else {
            _ctx.getClient().removeClientObserver(_logon);
            _ctx.getLocationDirector().removeLocationObserver(_location);
        }
    }
    
    /**
     * Handle the POP_VOLUME command.
     */
    public function handlePopVolume (trigger :Button) :void
    {
        if (VolumePopup.popupExists()) {
            VolumePopup.destroyCurrentInstance();
        } else {
            var popup :VolumePopup = new VolumePopup (trigger);
            popup.show();
        }
    }

    /**
     * Handle the MOVE_BACK command.
     */
    public function handleMoveBack (trigger :Button) :void
    {
        // The first item on the back stack is the current location. do we have
        // any other locations as well?
        if (_backstack.length > 1) {
            // Pop the current location... 
            _backstack.pop();
            // ...and pop the previous location as well. When we move to previous location,
            // it will be pushed back on top, as a result of attributeChanged notification.
            var previousId :Object = _backstack.pop();
            CommandEvent.dispatch(trigger, MsoyController.GO_SCENE, previousId);
        }
    }

    /**
     * Handle the EDIT_SCENE command.
     */
    public function handleEditScene () :void
    {
        if (canEditScene()) {
            var room :RoomView = _topPanel.getPlaceView() as RoomView;
            room.getRoomController().handleEditScene();
        }
    }   

    // IMPLEMENTATION DETAILS

    /** When location changes, and it's to the scene that we thought
        we should be entering, set the label appropriately. */
    protected function locationChanged (place :PlaceObject) :void
    {
        // is this a valid scene?
        var scene :Scene = _ctx.getSceneDirector().getScene();
        if (scene != null) {
            // Update the stack. Also, if this is not the first scene, enable the back button.
            var backEnabled :Boolean = false;
            if (_backstack != null) {
                _backstack.push(scene.getId());
                backEnabled = (_backstack.length > 1);
            }
            // Display location name, modify buttons
            _controlBar.updateNavigationWidgets(
                ((scene != null) ? scene.getName() : ""), backEnabled);
            _controlBar.sceneEditPossible = canEditScene();
            
        } else {
            // Invalid scene - don't even try to edit it! :)
            _controlBar.sceneEditPossible = false;
            _controlBar.updateNavigationWidgets("", false);
        }
    }

    /** When the logon changes (the client logs on or off), we reset
        their scene history. */
    protected function logonChanged (event :ClientEvent) :void
    {
        _backstack = new Array();
    }

    /** Can this scene be edited? */
    protected function canEditScene () :Boolean
    {
        var scene :MsoyScene = _ctx.getSceneDirector().getScene() as MsoyScene;
        return (scene != null && scene.canEdit(_ctx.getClientObject()));
    }

    /** World information. */
    protected var _ctx :WorldContext;

    /** The top panel control. */
    protected var _topPanel :TopPanel;
    
    /** Control bar that drives these actions. */
    protected var _controlBar :ControlBar;

    /** Back-stack of previously visited scenes. */
    protected var _backstack :Array;

    /** Adapter for the locationChanged function. */
    protected var _location :LocationAdapter;

    /** Adapter for the logonChanged function. */
    protected var _logon :ClientAdapter;
}
}
