package com.threerings.msoy.client {

import flash.display.InteractiveObject;
import flash.events.MouseEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;

import com.threerings.msoy.world.data.MsoyLocation;

public class RoomController extends SceneController
{
    // documentation inherited
    public override function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _mctx = (ctx as MsoyContext);
    }

    // documentation inherited
    protected override function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        var rp :RoomPanel = new RoomPanel(ctx as MsoyContext);
        _roomView = rp.view;
        return rp;
    }

    // documentation inherited
    public override function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
    }

    // documentation inherited
    public override function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);

        super.didLeavePlace(plobj);
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        trace("mouse clicked at : " + event.localX + ", " + event.localY);

        var newLoc :MsoyLocation = new MsoyLocation(
            event.localX, event.localY, 0, 0);
        _mctx.getSpotSceneDirector().changeLocation(newLoc, null);
    }

    protected var _mctx :MsoyContext;

    protected var _roomView :RoomView;
}
}
