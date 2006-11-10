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
     * should the avatar change.
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
        return parseNumber(query("orient"));
    }

    /**
     */
    public function isWalking () :Boolean
    {
        return parseBoolean(query("isWalking"));
    }

    /**
     * Essentially, we redispatch an event from the outside.
     */
    protected function handleAvatarChange (event :TextEvent) :void
    {
        // TODO: once we determine the final bits of data we'll be
        // passing to the avatar, we should just encode them all when we
        // send an avatarChanged from the avatar, then this would save the
        // encoded string and refer to that instead of having isWalking()
        // and getOrient() ask the avatar again.
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
