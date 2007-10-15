//
// $Id$

package com.threerings.msoy.chat.client {

import flash.geom.Rectangle;

import com.threerings.util.Name;

public interface ChatInfoProvider
{
    /**
     * Retun the rectangle bounds of the speaker
     * 
     * Null may be returned if the speaker is not known.
     */
    function getSpeakerBounds (speaker :Name) :Rectangle;

    /**
     * Add the bounding rectangles of things that should be avoided.
     */
    function getAvoidables (speaker :Name, high :Array, low :Array) :void;
}
}
