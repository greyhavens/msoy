package com.threerings.msoy.client {

import mx.containers.VBox;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

public class RoomView extends VBox
    implements PlaceView
{
    public function RoomView (ctx :MsoyContext)
    {
        width = 400;
        height = 300;
    }

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
    }
}
}
