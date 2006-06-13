package com.threerings.msoy.world.client {

import mx.containers.VBox;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.world.chat.client.ChatControl;
import com.threerings.msoy.world.chat.client.ChatTextArea;

public class RoomPanel extends VBox
    implements PlaceView
{
    /** The room view. */
    public var view :RoomView;

    public function RoomPanel (ctx :MsoyContext)
    {
        addChild(view = new RoomView(ctx));
        addChild(new ChatTextArea(ctx));
        addChild(new ChatControl(ctx));
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
