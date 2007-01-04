package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.errors.IllegalOperationError;

/**
 */
public class AvatarControl extends MsoyControl
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
    public function AvatarControl (disp :DisplayObject)
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

    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        o["avatarChanged_v1"] = avatarChanged_v1;
        o["avatarSpoke_v1"] = avatarSpoke_v1;
        o["action_v1"] = doAction_v1;
        o["getActions_v1"] = getActions_v1;
    }

    protected function avatarChanged_v1 (walking :Boolean, orient :Number) :void
    {
        _isWalking = walking;
        _orient = orient;
        if (avatarChanged != null) {
            avatarChanged();
        }
    }

    protected function avatarSpoke_v1 () :void
    {
        if (avatarSpoke != null) {
            avatarSpoke();
        }
    }

    /** 
     * Get the names of all the current actions.
     */
    protected function getActions_v1 () :Array
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
    protected function doAction_v1 (name :String) :void
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
