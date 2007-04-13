package com.threerings.msoy.world.client {

public interface ZOrderable
{
    /** Is this sprite included in standard layout in the room view? */
    function isIncludedInLayout () :Boolean;

    /** Return the Z value of this object, in room coordinates. */
    function getZ () :Number;
}
}
