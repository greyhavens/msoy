//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc.  Please do not redistribute.

package com.whirled {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.EventDispatcher;

/**
 * Base class for services that connect back to whirled.
 */
public class WhirledControl extends EventDispatcher
{
    /**
     */
    public function WhirledControl (disp :DisplayObject)
    {
        if (isAbstract()) {
            throw new Error("This control is abstract. Please use the " +
                "appropriate subclass: FurniControl, AvatarControl...");
        }

        var event :ConnectEvent = new ConnectEvent();
        var userProps :Object = new Object();
        populateProperties(userProps);
        event.userProps = userProps;
        disp.root.loaderInfo.sharedEvents.dispatchEvent(event);
        _props = event.hostProps;
        if (_props != null && "initProps" in _props) {
            gotInitProperties(_props["initProps"]);
            delete _props["initProps"]; // not needed after startup
        }

        disp.root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload, false, 0, true);
    }

    /**
     * Are we connected and running inside the whirled, or are we
     * merely being displayed standalone?
     */
    public function isConnected () :Boolean
    {
        return (_props != null);
    }

    /**
     * Populate any properties that we provide back to whirled.
     */
    protected function populateProperties (o :Object) :void
    {
        // nada
    }

    /**
     * Initialize/examine any properties sent from whirled after connecting.
     */
    protected function gotInitProperties (o :Object) :void
    {
        // nada
    }

    /**
     * Helper method to dispatch a ControlEvent, but only if there
     * is an associated listener.
     */
    protected function dispatch (ctrlEvent :String, key :String = null, value :Object = null) :void
    {
        if (hasEventListener(ctrlEvent)) {
            dispatchEvent(new ControlEvent(ctrlEvent, key, value));
        }
    }

    /**
     * Call an exposed function back in our hosting Whirled.
     */
    protected function callHostCode (name :String, ... args) :*
    {
        if (_props != null) {
            try {
                var func :Function = (_props[name] as Function);
                if (func != null) {
                    return func.apply(null, args);
                }

            } catch (err :Error) {
                trace(err.getStackTrace());
                trace("--");
                throw new Error("Unable to call host code: " + err.message);
            }
        }

        return undefined;
    }

    /**
     * Handle any shutdown required.
     */
    protected function handleUnload (evt :Event) :void
    {
        // nada
    }

    /**
     * Should we disallow instantiation of this class?
     */
    protected function isAbstract () :Boolean
    {
        return true;
    }

    /** The properties given us by our host. */
    protected var _props :Object;
}
}

import flash.events.Event;

/**
 * A special event we can use to pass info back to whirled.
 */
class ConnectEvent extends Event
{
    public function ConnectEvent ()
    {
        super("controlConnect", true, false);
    }

    /** Setter: hostProps */
    public function set hostProps (props :Object) :void
    {
        if (_parent != null) {
            _parent.hostProps = props;

        } else {
            _hostProps = props;
        }
    }

    /** Getter: hostProps */
    public function get hostProps () :Object
    {
        return _hostProps;
    }

    /** Setter: userProps */
    public function set userProps (props :Object) :void
    {
        _userProps = props;
    }

    /** Getter: userProps */
    public function get userProps () :Object
    {
        if (_parent != null) {
            return _parent.userProps;
        } else {
            return _userProps;
        }
    }

    override public function clone () :Event
    {
        var clone :ConnectEvent = new ConnectEvent();
        clone._parent = this;
        return clone;
    }

    protected var _parent :ConnectEvent;

    protected var _hostProps :Object;
    protected var _userProps :Object;
}
