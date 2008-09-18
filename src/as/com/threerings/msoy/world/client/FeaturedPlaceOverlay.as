//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Sprite;

import flash.events.MouseEvent;

import com.threerings.util.Command;
import com.threerings.util.MultiLoader;

import com.threerings.msoy.client.DeploymentConfig;
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

        if (_ctx.getMsoyClient().isEmbedded()) {
            if (_waterMark == null) {
                _waterMark = new Sprite();
                addChild(_waterMark);

                // NOTE: If watermark.png ever changes, update the width/height constants
                MultiLoader.getContents(
                    DeploymentConfig.serverURL + "rsrc/watermark.png",
                    _waterMark.addChild);
            }

            // Put it in bottom right, with a bit of padding
            _waterMark.x = width - WATERMARK_WIDTH - 4;
            _waterMark.y = height - WATERMARK_HEIGHT - 6;
        }
    }

    /** Now with 20% more context. */
    protected var _ctx :MsoyContext;

    protected var _waterMark :Sprite;

    // These need to be known ahead of time for layout purposes
    protected static const WATERMARK_WIDTH :int = 57;
    protected static const WATERMARK_HEIGHT :int = 35;
}

}
