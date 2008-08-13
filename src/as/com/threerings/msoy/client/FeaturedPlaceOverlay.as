//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.MouseEvent;

import com.threerings.util.Command;

public class FeaturedPlaceOverlay extends Sprite
    implements PlaceLayer
{
    public function FeaturedPlaceOverlay (ctx :MsoyContext)
    {
        _ctx = ctx;

        // Required by overlay manager
        name = "featuredPlaceOverlay";

        // Mostly just to get the hand cursor
        buttonMode = true;

        Command.bind(this, MouseEvent.CLICK, MsoyController.FEATURED_PLACE_CLICKED);
    }

    public function setPlaceSize (width :Number, height :Number) :void
    {
        // Create a clickable invisible overlay
        graphics.beginFill(0, 0);
        graphics.drawRect(0, 0, width, height);
        graphics.endFill();

        if (_ctx.getMsoyClient().isEmbedded()) {
            if (_waterMark == null) {
                _waterMark = new WATER_MARK() as DisplayObject;
                _waterMark.alpha = 0.5; // Hack until we get a real watermark graphic
                addChild(_waterMark);
            }

            // Put it in bottom right
            _waterMark.x = width - _waterMark.width;
            _waterMark.y = height - _waterMark.height;
        }
    }

    /** Now with 20% more context. */
    protected var _ctx :MsoyContext;

    protected var _waterMark :DisplayObject;

    // TODO: Get real watermark
    [Embed(source="../../../../../../rsrc/media/skins/embedheader/logo.jpg")]
    protected static const WATER_MARK :Class;
}

}
