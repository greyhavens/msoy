//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.MouseEvent;

import com.threerings.util.Command;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceLayer;

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

        Command.bind(this, MouseEvent.CLICK, WorldController.FEATURED_PLACE_CLICKED);
    }

    public function setPlaceSize (width :Number, height :Number) :void
    {
        // Create a clickable invisible overlay
        graphics.beginFill(0, 0);
        graphics.drawRect(0, 0, width, height);
        graphics.endFill();

// TEMP: reenable when watermark.png is committed
//
//         if (_ctx.getMsoyClient().isEmbedded()) {
//             if (_waterMark == null) {
//                 _waterMark = new WATER_MARK() as DisplayObject;
//                 addChild(_waterMark);
//             }

//             // Put it in bottom right, with a bit of padding
//             _waterMark.x = width - _waterMark.width - 4;
//             _waterMark.y = height - _waterMark.height - 6;
//         }
    }

    /** Now with 20% more context. */
    protected var _ctx :MsoyContext;

    protected var _waterMark :DisplayObject;

//     [Embed(source="../../../../../../../../rsrc/media/skins/watermark.png")]
//     protected static const WATER_MARK :Class;
}

}
