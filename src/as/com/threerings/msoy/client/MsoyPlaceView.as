package com.threerings.msoy.client {

import com.threerings.crowd.client.PlaceView;

public interface MsoyPlaceView extends PlaceView
{
    /**
     * Set the size of the placeview.
     */
    function setPlaceSize (
        unscaledWidth :Number, unscaledHeight :Number) :void;

    /**
     * Tell the placeview whether it should show a chatbox or not.
     */
    function setChatOverlayEnabled (enabled :Boolean) :void;
}
}
