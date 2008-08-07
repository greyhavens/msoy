//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.Stage;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.room.data.RoomConfig;

public class StudioClient extends WorldClient
{
    public function StudioClient (stage :Stage, params :Object)
    {
        super(stage);

        _roomStudioController = new RoomStudioController();
        _roomStudioController.init(_wctx, new RoomConfig());
        _wctx.getTopPanel().setPlaceView(_roomStudioController.getPlaceView());
    }

    public function getPlaceView () :RoomStudioView
    {
        return _roomStudioController.getPlaceView() as RoomStudioView;
    }

    override public function getHostname () :String
    {
        // we do this to trick WorldClient into calling logon().
        return "studio";
    }

    override public function logon () :Boolean
    {
        // we do nothing here
        return false;
    }

    // from WorldClient
    override protected function createContext () :MsoyContext
    {
        return (_wctx = new StudioContext(this));
    }

    protected var _roomStudioController :RoomStudioController;
}
}
