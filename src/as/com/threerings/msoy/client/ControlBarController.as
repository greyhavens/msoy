//
// $Id$

package com.threerings.msoy.client {

import flash.events.IEventDispatcher;
import mx.controls.Button;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.SessionObserver;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.data.ClientObject;

import com.threerings.util.CommandEvent;
import com.threerings.util.Controller;


/**
 * Controller for actions from the ControlBar UI.
 *
 * The controller takes care of actions specific to the control bar (e.g. volume popup).
 * Also, it keeps track of the client's movement through the scenes, and supports
 * movement "back" through the stack of previously visited scenes, akin to the back button
 * in a web browser.
 */
public class ControlBarController extends Controller
    implements SessionObserver, AttributeChangeListener
{
    public static const log :Log = Log.getLog(ControlBarController);

    /** Command to display a volume slider. */
    public static const POP_VOLUME :String = "handlePopVolume";

    /** Command to move back to the previous location. */
    public static const MOVE_BACK :String = "handleMoveBack";

    /**
     * Create the controller.
     */
    public function ControlBarController (ctx :WorldContext, controlBar :ControlBar)
    {
        _ctx = ctx;
        _controlBar = controlBar;
        
        setControlledPanel(ctx.getStage());
    }

    /**
     * Registers or unregisters for updates on the client's logging on or off.
     *
     * This is the first step of setting up for scene monitoring:
     * 1. When the ControlBar is created, this function is called, and registers
     *    the controller for observations about the client's logon/logoff events.
     * 2. When the client logs on, we register for updates on their sceneId property.
     * 3. When the sceneId changes, it means they moved to a new scene, so we push
     *    the new value on the back stack.
     */
    public function registerForSessionObservations (registration :Boolean) :void
    {
        if (registration) {
            _ctx.getClient().addClientObserver(this);
        } else {
            _ctx.getClient().removeClientObserver(this);
        }
    }
    
    /**
     * Handle the POP_VOLUME command.
     */
    public function handlePopVolume (trigger :Button) :void
    {
        var popup :VolumePopup = new VolumePopup (trigger);
        popup.show();
    }

    /**
     * Handle the MOVE_BACK command.
     */
    public function handleMoveBack (trigger :Button) :void
    {
        // The first item on the back stack is the current location. do we have
        // any other locations as well?
        if (_backstack.length > 1)
        {
            // Pop the current location... 
            _backstack.pop ();
            // ...and pop the previous location as well. When we move to previous location,
            // it will be pushed back on top, as a result of attributeChanged notification.
            var previousId :Object = Object(_backstack.pop());
            CommandEvent.dispatch (trigger, MsoyController.GO_SCENE, previousId);
        }
    }
    
    // from interface SessionObserver
    public function clientWillLogon (event :ClientEvent) :void
    {
        // don't do anything - we don't care about this case
    }

    // from interface SessionObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        // get the player's member object, and register for attribute changes
        var clientObj :ClientObject = event.getClient().getClientObject();
        if (clientObj != null) {
            _backstack = new Array ();
            clientObj.addListener(this);
        }
    }

    // from interface SessionObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // don't do anything - we don't care about this case
    }

    // from interface SessionObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        // remove this object as a listener on the player's attributes
        var clientObj :ClientObject = event.getClient().getClientObject();
        if (clientObj != null) {
            clientObj.removeListener(this);
        }
    }

    // from interface AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        // if this is a new scene, just push it on the back stack
        if (event.getName() == "sceneId") {
            _backstack.push (event.getValue());

            // also, if this is the first scene, disable the back button
            _controlBar.backMovementPossible = (_backstack.length > 1);
        }
    }

    // IMPLEMENTATION DETAILS


    

    /** World information. */
    protected var _ctx :WorldContext;

    /** Control bar that drives these actions. */
    protected var _controlBar :ControlBar;

    /** Back-stack of previously visited scenes. */
    protected var _backstack :Array;

}

}

