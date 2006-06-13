package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import com.threerings.events.ControllerEvent;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.world.data.MsoyPortal;

import flash.events.TimerEvent;
import flash.utils.Timer;

public class PortalSprite extends MsoySprite
{
    public function PortalSprite (portal :MsoyPortal)
    {
        super(portal.media);
        _portal = portal;
        mouseChildren = false;

        // TEMP: testing
        /*
        var timer :Timer = new Timer(2000);
        timer.addEventListener(TimerEvent.TIMER,
            function (evt :TimerEvent) :void {
                wasTraversed(Math.random() > .5);
            });
        timer.start();
        */
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

    override public function isInteractive () :Boolean
    {
        // force all portals to be interactive
        return true;
    }

    override protected function getHoverColor () :uint
    {
        return 0xe04040; // red
    }

    protected var _portal :MsoyPortal;
}

}
