package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import com.threerings.events.ControllerEvent;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.MediaData;

public class PortalMedia extends ScreenMedia
{
    public function PortalMedia (portal :Portal)
    {
        super(new MediaData(3));
        _portal = portal;
    }

    public function wasTraversed (entering :Boolean) :void
    {
        sendMessage("action", entering ? "bodyEntered" : "bodyLeft");
    }

    // documentation inherited
    override protected function mouseClick (event :MouseEvent) :void
    {
        dispatchEvent(new ControllerEvent("portalClicked", _portal));
    }

    override protected function isInteractive () :Boolean
    {
        // force all portals to be interactive
        return true;
    }

    override protected function getHoverColor () :uint
    {
        return 0xe04040; // red
    }

    protected var _portal :Portal;
}

}
