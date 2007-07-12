//
// $Id$

package com.threerings.msoy.chat.client {

import flash.geom.Rectangle;

import com.threerings.util.Name;

public interface ChatInfoProvider
{
    /**
     * @return an array containing the following pieces of info:
     * [ rectangle, point, distance ], all in screen coordinates.
     * The rectangle is the bounds of the speaker's avatar, the point is the mouthspot,
     * and the distance is the preferred tail termination distance.
     * 
     * Null may be returned if the speaker is not known.
     */
    function getSpeakerInfo (speaker :Name) :Array;

    /**
     * Add the bounding rectangles of things that should be avoided.
     */
    function getAvoidables (speaker :Name, high :Array, low :Array) :void;
}
}
