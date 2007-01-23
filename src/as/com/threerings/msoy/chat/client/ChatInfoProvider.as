package com.threerings.msoy.chat.client {

import flash.geom.Rectangle;

import com.threerings.util.Name;

public interface ChatInfoProvider
{
    /**
     * @return a rectangle representing the screen-coord bounds
     * of the specified speaker, or null if not known.
     */
    function getSpeaker (speaker :Name) :Rectangle;

    /**
     * Add the bounding rectangles of things that should be avoided.
     */
    function getAvoidables (speaker :Name, high :Array, low :Array) :void;
}
}
