package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import com.threerings.mx.events.CommandEvent;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.world.data.MsoyPortal;

import flash.events.TimerEvent;
import flash.utils.Timer;

public class PortalSprite extends MsoySprite
{
    public function PortalSprite (portal :MsoyPortal)
    {
        _portal = portal;
        super(portal.media);
    }

    public function wasTraversed (entering :Boolean) :void
    {
        sendMessage("action", entering ? "bodyEntered" : "bodyLeft");
    }

    public function getPortal () :MsoyPortal
    {
        return _portal;
    }

    public function update (portal :MsoyPortal) :void
    {
        _portal = portal;
        setup(portal.media);
        scaleUpdated();
        setLocation(portal.loc);
    }

    override public function setEditing (editing :Boolean) :void
    {
        // clone the portal data so that we can safely modify it, both
        // when editing and prior to resetting
        _portal = (_portal.clone() as MsoyPortal);

        super.setEditing(editing);
    }

    // documentation inherited
    override protected function mouseClick (event :MouseEvent) :void
    {
        CommandEvent.dispatch(this, RoomController.PORTAL_CLICKED, _portal);
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
        _portal.scaleX = scaleX;
        scaleUpdated();
    }

    override public function setMediaScaleY (scaleY :Number) :void
    {
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
