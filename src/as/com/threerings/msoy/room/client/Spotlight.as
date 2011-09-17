//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.display.MovieClip;
import flash.display.Sprite;
import flash.events.Event;

import com.threerings.display.DisplayUtil;

import com.threerings.msoy.room.data.MsoyLocation;

public class Spotlight
{
    public function Spotlight (view :RoomView, follow :OccupantSprite)
    {
        _view = view;
        _follow = follow;
        _loc = new MsoyLocation();

        _under = new SpotlightElement();
        _under.addChild(new UNDER());
        _view.addElement(_under);
        recursiveStop(_under);

        _over = new SpotlightElement();
        _over.addChild(new OVER());
        _view.addElement(_over);
        recursiveStop(_over);

        _follow.viz.addEventListener(Event.ENTER_FRAME, onEnterFrame);
        onEnterFrame();
    }

    public function shutdown () :void
    {
        _follow.viz.removeEventListener(Event.ENTER_FRAME, onEnterFrame);

        _view.removeElement(_under);
        _view.removeElement(_over);
    }

    protected function onEnterFrame (event :Event = null) :void
    {
        var loc :MsoyLocation = _follow.getLocation();
        if (_loc.equals(loc)) {
            return; // They haven't moved
        }

        _loc.set(loc);

        var overLoc :MsoyLocation = _loc.clone() as MsoyLocation;
        overLoc.z -= 0.001;
        _over.setLocation(overLoc);

        var underLoc :MsoyLocation = _loc.clone() as MsoyLocation;
        underLoc.z += 0.001;
        _under.setLocation(underLoc);

        _view.locationUpdated(_over);
        _view.locationUpdated(_under);
    }

    public static function recursiveStop (root :DisplayObject) :void
    {
        DisplayUtil.applyToHierarchy(root, function (disp :DisplayObject) :void {
            if (disp is MovieClip) {
                // TODO(bruno): Recalculate this every frame, taking the room perspective into account
                (disp as MovieClip).gotoAndStop(6);
            }
        });
    }

    [Embed(source="../../../../../../../rsrc/media/spotlight.swf", symbol="spotlight_under")]
    protected static const UNDER :Class;
    [Embed(source="../../../../../../../rsrc/media/spotlight.swf", symbol="spotlight_over")]
    protected static const OVER :Class;

    protected var _under :SpotlightElement;
    protected var _over :SpotlightElement;

    protected var _view :RoomView;
    protected var _follow :OccupantSprite;
    protected var _loc :MsoyLocation;
}
}

import com.threerings.msoy.room.client.RoomElementSprite;
import com.threerings.msoy.room.data.RoomCodes;

class SpotlightElement extends RoomElementSprite
{
    override public function getRoomLayer () :int
    {
        return RoomCodes.FURNITURE_LAYER;
    }
}
