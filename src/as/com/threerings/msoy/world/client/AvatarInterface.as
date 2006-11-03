package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.TextEvent;

/**
 */
public class AvatarInterface extends MsoyInterface
{
    /**
     * A place where you can attach a function that will get called
     * should the avatar
     */
    public var avatarChanged :Function;

    /**
     */
    public function AvatarInterface (disp :DisplayObject)
    {
        super(disp);

        _dispatcher.addEventListener("msoyAvatarChange", handleAvatarChange);
    }

    /**
     * Get the current orientation of our avatar.
     *
     * @return a value between 0 - 360.
     */
    public function getOrientation () :Number
    {
        return Number(query("orient"));
        // Number(null) == 0, if we're disconnected
    }

    /**
     */
    public function isWalking () :Boolean
    {
        return Boolean(query("isWalking"));
        // Boolean(null) == false, if we're disconnected
    }

    /**
     * Essentially, we redispatch an event from the outside.
     */
    protected function handleAvatarChange (event :TextEvent) :void
    {
        if (avatarChanged != null) {
            avatarChanged();
        }
    }

    override protected function handleUnload (event :Event) :void
    {
        _dispatcher.removeEventListener("msoyAvatarChange", handleAvatarChange);
        super.handleUnload(event);
    }
}
}
