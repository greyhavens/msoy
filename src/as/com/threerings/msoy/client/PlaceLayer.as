//
// $Id: MsoyPlaceView.as 6166 2007-10-12 19:15:51Z mdb $

package com.threerings.msoy.client {

import com.threerings.crowd.client.PlaceView;

/**
 * An interface implemented by components that wish to reside inside a PlaceView panel
 * and thus respond to changes in that panel's dimensions.
 */
public interface PlaceLayer
{
    /**
     * Informs the place view of its pixel dimensions.
     */
    function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void;
}
}
