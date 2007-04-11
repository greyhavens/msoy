package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Returned by AbstractRoomView's pointToLocation().
 * Encodes a world location, as well as information about where the click
 * actually landed so that entities can make informed decisions about
 * what to do with the location.
 */
public class ClickLocation
{
    public static const FLOOR :int = 0;
    public static const BACK_WALL :int = 1;
    public static const CEILING :int = 2;
    public static const LEFT_WALL :int = 3;
    public static const RIGHT_WALL :int = 4;

    /** Where the click actually landed. */
    public var click :int;

    /** The world coordinate of the click. */
    public var loc :MsoyLocation;

    /**
     * Construct a ClickLocation.
     */
    public function ClickLocation (click :int, loc :MsoyLocation)
    {
        this.click = click;
        this.loc = loc;
    }
}
}
