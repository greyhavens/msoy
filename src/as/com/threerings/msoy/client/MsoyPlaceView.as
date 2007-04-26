//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.client.PlaceView;

/**
 * An expanded PlaceView interface that can be used by views that wish to learn about their actual
 * pixel dimensions.
 */
public interface MsoyPlaceView extends PlaceView
{
    /**
     * Informs the place view of its pixel dimensions.
     */
    function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void;
}
}
