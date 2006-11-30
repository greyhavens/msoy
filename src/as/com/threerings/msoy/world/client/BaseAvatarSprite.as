package com.threerings.msoy.world.client {

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.world.data.MsoyLocation;

public class BaseAvatarSprite extends MsoySprite
{
    /** The maximum width of an avatar sprite. */
    public static const MAX_WIDTH :int = 300;

    /** The maximum height of an avatar sprite. */
    public static const MAX_HEIGHT :int = 400;

    public function BaseAvatarSprite (desc :MediaDesc)
    {
        super(desc);
    }

    /**
     * Called to set up the avatar's initial location upon entering
     * a room.
     */
    public function setEntering (loc :MsoyLocation) :void
    {
        setLocation(loc);
        setOrientation(loc.orient);
        stanceDidChange();
    }

    override public function getMaxContentWidth () :int
    {
        return MAX_WIDTH;
    }

    override public function getMaxContentHeight () :int
    {
        return MAX_HEIGHT;
    }

    /**
     * @return true if we're moving.
     */
    public function isMoving () :Boolean
    {
        throw new Error("abstract");
    }

    public function setOrientation (orient :int) :void
    {
        loc.orient = orient;
    }

    override public function isInteractive () :Boolean
    {
        return true;
    }

    override public function hasAction () :Boolean
    {
        return true;
    }
    
    /**
     * Called when the avatar changes orientation or transitions between
     * walking or standing.
     */
    protected function stanceDidChange () :void
    {
        sendMessage("avatarChanged", [ isMoving(), loc.orient ]);
    }
}
}
