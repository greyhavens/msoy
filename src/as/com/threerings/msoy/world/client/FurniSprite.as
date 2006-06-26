package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import flash.net.URLRequest;
import flash.net.navigateToURL;

import com.threerings.msoy.world.data.FurniData;

public class FurniSprite extends MsoySprite
{
    public function FurniSprite (furni :FurniData)
    {
        _furni = furni;
        super(furni.media);

        configureAction();
    }

    override public function getMediaScaleX () :Number
    {
        return _furni.scaleX;
    }

    override public function getMediaScaleY () :Number
    {
        return _furni.scaleY;
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
    override public function hasAction () :Boolean
    {
        return (_furni.action != null);
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

    /**
     * Do any setup required for the furniture's action.
     */
    protected function configureAction () :void
    {
        var action :Object = _furni.action;
        if (action == null) {
            return;
        }

        // set our dest url as a tooltip..
        if (action is String) {
            toolTip = (action as String);

        } else {
            Log.getLog(this).warning("Don't understand furniture action " +
                "[action=" + action + "].");
        }
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;
}
}
