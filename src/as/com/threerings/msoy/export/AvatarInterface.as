package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.errors.IllegalOperationError;

import flash.events.Event;

/**
 */
public class AvatarInterface extends MsoyInterface
{
    /**
     * A place where you can attach a function that will get called
     * should the avatar change.
     */
    public var avatarChanged :Function;

    /**
     * Called once when the avatar speaks.
     */
    public var avatarSpoke :Function;

    /**
     */
    public function AvatarInterface (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Add named actions that can be used to animate this avatar.
     */
    // TODO: review this: perhaps we make it so that actions can
    // only be added at construction time? What's to stop someone from making
    // more than one AvatarInterface?
    public function addAction (name :String, callback :Function) :void
    {
        if (name == null || name.length > 20) {
            throw new IllegalOperationError("Invalid name: null or too long.");
        }
        _actions[name] = callback;
    }

    /**
     * Get the current orientation of our avatar.
     *
     * @return a value between 0 - 360.
     */
    public function getOrientation () :Number
    {
        return _orient;
    }

    /**
     * Is the avatar currently walking?
     */
    public function isWalking () :Boolean
    {
        return _isWalking;
    }

    override protected function handleQuery (name :String, val :Object) :Object
    {
        switch (name) {
        case "avatarChanged":
            _isWalking = Boolean(val[0]);
            _orient = Number(val[1]);
            if (avatarChanged != null) {
                avatarChanged();
            }
            return null;

        case "avatarSpoke":
            if (avatarSpoke != null) {
                avatarSpoke();
            }
            return null;

        case "action":
            doAction(val as String);
            return null;

        case "getActions":
            return getActions();

        default:
            return super.handleQuery(name, val);
        }
    }

    /** 
     * Get the names of all the current actions.
     */
    protected function getActions () :Array
    {
        var actions :Array = [];
        for (var name :String in _actions) {
            actions.push(name);
        }
        return actions;
    }

    /**
     * Have an avatar do the action.
     */
    protected function doAction (name :String) :void
    {
        // try invoking the callback function for the action.
        try {
            _actions[name]();
        } catch (e :Error) {
            // cry not
        }
    }

    /** Are we currently walking? */
    protected var _isWalking :Boolean;

    /** Our current orientation, or 0. */
    protected var _orient :Number;

    /** An associative hash of callback functions, indexed by action name. */
    protected var _actions :Object = new Object();
}
}
