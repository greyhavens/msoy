package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import com.threerings.events.ControllerEvent;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.MediaData;

public class PortalMedia extends ScreenMedia
{
    public function PortalMedia (portal :Portal)
    {
        super(new MediaData(2));
        _portal = portal;
    }

    // documentation inherited
    protected override function mouseClick (event :MouseEvent) :void
    {
        dispatchEvent(new ControllerEvent("portalClicked", _portal));
    }

    protected var _portal :Portal;
}

}
