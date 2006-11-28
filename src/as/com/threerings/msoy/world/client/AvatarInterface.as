package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

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
     */
    public function AvatarInterface (disp :DisplayObject)
    {
        super(disp);
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

        default:
            return super.handleQuery(name, val);
        }
    }

    /** Are we currently walking? */
    protected var _isWalking :Boolean;

    /** Our current orientation, or 0. */
    protected var _orient :Number;
}
}
