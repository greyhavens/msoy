//
// $Id$

package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.errors.IllegalOperationError;

/**
 * Defines the mechanism by which avatars interact with the world view.
 */
public class AvatarControl extends ActorControl
{
    /**
     */
    public function AvatarControl (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Set named actions that can be used to animate this avatar.
     */
    public function setActions (actionName :String, ... moreActions) :void
    {
        moreActions.unshift(actionName);
        for (var ii :int = 0; ii < moreActions.length; ii++) {
            if (!(moreActions[ii] is String)) {
                throw new ArgumentError("All actions must be Strings.");
            }
            for (var jj :int = 0; jj < ii; jj++) {
                if (moreActions[jj] === moreActions[ii]) {
                    throw new ArgumentError("Duplicate action specified: " +
                        moreActions[ii]);
                }
            }
        }
        _actions = moreActions;
    }

    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        o["avatarSpoke_v1"] = avatarSpoke_v1;
        o["action_v1"] = doAction_v1;
        o["getActions_v1"] = getActions_v1;
    }

    protected function avatarSpoke_v1 () :void
    {
        dispatchEvent(new ControlEvent(ControlEvent.AVATAR_SPOKE));
    }

    /** 
     * Get the names of all the current actions.
     */
    protected function getActions_v1 () :Array
    {
        return _actions;
    }

    /**
     * Have an avatar do the action.
     */
    protected function doAction_v1 (name :String) :void
    {
        dispatchEvent(new ControlEvent(ControlEvent.ACTION_TRIGGERED, name));
    }

    /** An array of all action names. */
    protected var _actions :Array = [];
}
}
