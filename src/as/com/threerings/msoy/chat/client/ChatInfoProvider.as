//
// $Id$

package com.threerings.msoy.chat.client {

import flash.geom.Point;

import com.threerings.util.Name;

public interface ChatInfoProvider
{
    /**
     * Return the position to place bubbles attributed to this speaker.
     *
     * Null may be returned if the speaker is not known.
     */
    function getBubblePosition (speaker :Name) :Point;
}
}
