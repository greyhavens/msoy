//
// $Id$

package com.threerings.msoy.client {

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
    public function init (ctx :MsoyContext, loader :Loader) :void
    {
        _ctx = ctx;
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

            } catch (err :*) {
                Log.getLog(this).warning("Error in usercode", err);
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
        // older code has the properies in the top-level event, newer code in a sub-object
        var props :Object = ("props" in evt) ? evt.props : evt;

        if (_props != null) {
            // attempt to report this back to the caller, but don't worry if we can't
            // set the property, old APIs only allowed userProps and hostProps.
            try {
                props.alreadyConnected = true;
                return;
            } catch (err :Error) {
                // Let's log something for our own edification when this happens, but note well
                // that this is not an error. Old avatars could be connecting twice and coping,
                // so we don't want to break them. We use the above "alreadyConnected" property
                // to inform newer entities that they're booching it.
                var log :Log = Log.getLog(this);
                log.warning("Warning: Usercode connected more than once. [backend=" + this + "].");
            }
        }

        // copy down the user functions
        setUserProperties(props.userProps);
        // pass back ours
        var hostProps :Object = new Object();
        populateControlProperties(hostProps);
        var initProps :Object = new Object();
        populateControlInitProperties(initProps);
        hostProps["initProps"] = initProps;
        props.hostProps = hostProps;
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
        o["startTransaction"] = startTransaction_v1;
        o["commitTransaction"] = commitTransaction_v1;
    }

    /**
     * Populate any properties that will only be needed when the control
     * is first initialized.
     */
    protected function populateControlInitProperties (o :Object) :void
    {
        // nothing by default
    }

    /**
     * Starts a transaction that will group all invocation requests into a single message.
     */
    protected function startTransaction_v1 () :void
    {
        if (_ctx != null) { // _ctx may be null in the avatarviewer, places like that
            _ctx.getClient().getInvocationDirector().startTransaction();
        }
    }

    /**
     * Commits a transaction started with {@link #startTransaction_v1}.
     */
    protected function commitTransaction_v1 () :void
    {
        if (_ctx != null) { // _ctx may be null in the avatarviewer, places like that
            _ctx.getClient().getInvocationDirector().commitTransaction();
        }
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** Properties populated by usercode. */
    protected var _props :Object;

    /** The event dispatcher we share with the usercode. Use a jolly! */
    protected var _sharedEvents :EventDispatcher;
}
}
