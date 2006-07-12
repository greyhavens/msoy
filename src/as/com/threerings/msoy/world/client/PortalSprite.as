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
        _portal = portal;
        super(portal.media);
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

    public function getPortal () :MsoyPortal
    {
        return _portal;
    }

    // documentation inherited
    override protected function mouseClick (event :MouseEvent) :void
    {
        dispatchEvent(new ControllerEvent("portalClicked", _portal));
    }

    override public function getMediaScaleX () :Number
    {
        return _portal.scaleX;
    }

    override public function getMediaScaleY () :Number
    {
        return _portal.scaleY;
    }

    override public function setMediaScaleX (scaleX :Number) :void
    {
        // clone the portal once we start modifying it
        _portal = (_portal.clone() as MsoyPortal);
        _portal.scaleX = scaleX;
        scaleUpdated();
    }

    override public function setMediaScaleY (scaleY :Number) :void
    {
        _portal = (_portal.clone() as MsoyPortal);
        _portal.scaleY = scaleY;
        scaleUpdated();
    }

    override public function get maxContentWidth () :int
    {
        return 800;
    }

    override public function get maxContentHeight () :int
    {
        return 600;
    }

    override public function isInteractive () :Boolean
    {
        // force all portals to be interactive
        return true;
    }

    override public function hasAction () :Boolean
    {
        // all portals have action
        return true;
    }

    override protected function getHoverColor () :uint
    {
        return 0xe04040; // red
    }

    protected var _portal :MsoyPortal;
}
}
