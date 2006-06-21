package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import flash.net.URLRequest;
import flash.net.navigateToURL;

import com.threerings.msoy.world.data.FurniData;

public class FurniSprite extends MsoySprite
{
    public function FurniSprite (furni :FurniData)
    {
        super(furni.media);
        _furni = furni;

        // set our dest url as a tooltip..
        if (_furni.action is String) {
            toolTip = (_furni.action as String);
        }
    }

    override public function get maxContentWidth () :int
    {
        return 2000;
    }

    override public function get maxContentHeight () :int
    {
        return 1000;
    }

    // documentation inherited
    override public function isInteractive () :Boolean
    {
        return _desc.isInteractive();
    }

    // documentation inherited
    override protected function getHoverColor () :uint
    {
        return 0xe0e040; // yellow
    }

    // documentation inherited
    override protected function mouseClick (event :MouseEvent) :void
    {
        if (_furni.action is String) {
            navigateToURL(new URLRequest(_furni.action as String), "_self");
        }
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;
}
}
