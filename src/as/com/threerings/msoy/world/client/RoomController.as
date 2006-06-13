package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.ui.Keyboard;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.world.data.MsoyLocation;

public class RoomController extends SceneController
{
    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _mctx = (ctx as MsoyContext);
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        var rp :RoomPanel = new RoomPanel(ctx as MsoyContext);
        _roomView = rp.view;
        return rp;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        super.didLeavePlace(plobj);
    }

    override public function handleAction (cmd :String, arg :Object) :Boolean
    {
        if (cmd == "portalClicked") {
            var portal :Portal = (arg as Portal);
            _mctx.getSpotSceneDirector().traversePortal(portal.portalId);
            return true;
        }

        return super.handleAction(cmd, arg);
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        if (_roomView.isLocationTarget(event.target as DisplayObject)) {
            // mouse events are propogated upwards to parent components
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();

            // calculate where the location is
            var newLoc :MsoyLocation = _roomView.pointToLocation(
                event.localX, event.localY);
            if (newLoc != null) {
                // orient the location as appropriate
                newLoc.orient = (curLoc.x > newLoc.x ? 180 : 0);
                _mctx.getSpotSceneDirector().changeLocation(newLoc, null);
            }
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.SHIFT:
            _roomView.dimAvatars(event.type == KeyboardEvent.KEY_DOWN);
            break;

        case Keyboard.CONTROL:
            _roomView.dimFurni(event.type == KeyboardEvent.KEY_DOWN);
            break;
        }
    }

    protected var _mctx :MsoyContext;

    protected var _roomView :RoomView;
}
}
