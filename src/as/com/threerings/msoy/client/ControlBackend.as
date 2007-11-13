//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.EventDispatcher;

import flash.display.Loader;

import com.threerings.util.Log;

/**
 * The base class for communicating with MsoyControl instances
 * that live in usercode.
 */
public class ControlBackend
{
    /**
     * Initialize a backend to safely communicate with usercode.
     */
    public function init (loader :Loader) :void
    {
        _sharedEvents = loader.contentLoaderInfo.sharedEvents;
        _sharedEvents.addEventListener("controlConnect", handleUserCodeConnect, false, 0, true);
    }

    /**
     * Call an exposed function in usercode.
     */
    public function callUserCode (name :String, ... args) :*
    {
        if (_props != null) {
            try {
                var func :Function = (_props[name] as Function);
                if (func != null) {
                    return func.apply(null, args);
                }

            } catch (err :Error) {
                var log :Log = Log.getLog(this);
                log.warning("Error in user-code: " + err);
                log.logStackTrace(err);
            }
        }
        return undefined;
    }

    /**
     * Did the usercode expose a function with the specified name?
     */
    public function hasUserCode (name :String) :Boolean
    {
        return (_props != null) && (_props[name] is Function);
    }

    /**
     * Shutdown and disconnect this control.
     */
    public function shutdown () :void
    {
        _sharedEvents.removeEventListener("controlConnect", handleUserCodeConnect);
        _sharedEvents = null;
        _props = null;
    }

    /**
     * Handle an event from usercode, hook us up!
     */
    protected function handleUserCodeConnect (evt :Object) :void
    {
        // copy down the user functions
        setUserProperties(evt.userProps);
        // pass back ours
        var hostProps :Object = new Object();
        populateControlProperties(hostProps);
        var initProps :Object = new Object();
        populateControlInitProperties(initProps);
        hostProps["initProps"] = initProps;
        evt.hostProps = hostProps;
    }

    /**
     * Retain a reference to the ball of functions we've received
     * from usercode.
     */
    protected function setUserProperties (o :Object) :void
    {
//        // prototype for backwards compatability:
//        var oldFunc :Function = (o["avatarChanged_v1"] as Function);
//        if (oldFunc != null) {
//            // make a new function that adapts to the old one
//            o["avatarChanged_v2"] =
//                function (moving :Boolean, orient :Number, newParam :String)
//                :void {
//                    oldFunc(moving, orient);
//                };
//        }

        // then, simply save the properties
        _props = o;
    }

    /**
     * Populate the properties we pass back to user-code.
     */
    protected function populateControlProperties (o :Object) :void
    {
        // nothing by default
    }

    /**
     * Populate any properties that will only be needed when the control
     * is first initialized.
     */
    protected function populateControlInitProperties (o :Object) :void
    {
        // nothing by default
    }

    /** Properties populated by usercode. */
    protected var _props :Object;

    /** The event dispatcher we share with the usercode. Use a jolly! */
    protected var _sharedEvents :EventDispatcher;
}
}
