//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.geom.Point;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.world.data.DecorData;

/**
 * Helper class that draws a room backdrop with four walls, floor, and ceiling.
 */
public class RoomBackdrop
{
    /** Refresh room metrics. */
    public function setRoom (data :DecorData) :void
    {
        _metrics.update(data);
        _roomType = data.type;
    }

    /**
     * Draws the room backdrop on the specified sprite, with specified unscaled parameters.
     */
    public function drawRoom (
        sprite :Sprite, unscaledWidth :Number, unscaledHeight :Number, editing :Boolean) :void
    {
        var g :Graphics = sprite.graphics;
        g.clear();

        // fill all our screen area with transparent pixels, so that mousing anywhere in our bounds
        // includes us in the event dispatch. This is primarily necessary to get the ContextMenu
        // working properly.
        if (!isNaN(unscaledWidth) && !isNaN(unscaledHeight)) {
            g.beginFill(0, 0);
            g.drawRect(0, 0, unscaledWidth, unscaledHeight);
            g.endFill();
        }

        var drawWalls :Boolean = (_roomType == Decor.DRAWN_ROOM);
        var drawEdges :Boolean = drawWalls || editing;
        if (!drawEdges) {
            return; // nothing to draw
        }

        // variable names: [l/r][l/u][f/b] - left/right, lower/upper, front/back
        // Note: you can verify that a line connects two corners along an edge by checking that the
        // two endpoints differ in only one letter.
        var llf :Point = _metrics.roomToScene(0, 0, 0);
        var llb :Point = _metrics.roomToScene(0, 0, 1);
        var luf :Point = _metrics.roomToScene(0, 1, 0);
        var lub :Point = _metrics.roomToScene(0, 1, 1);

        var rlf :Point = _metrics.roomToScene(1, 0, 0);
        var rlb :Point = _metrics.roomToScene(1, 0, 1);
        var ruf :Point = _metrics.roomToScene(1, 1, 0);
        var rub :Point = _metrics.roomToScene(1, 1, 1);

        if (drawWalls) {
            // fill in the floor
            g.beginFill(0x333333);
            g.moveTo(llf.x, llf.y);
            g.lineTo(llb.x, llb.y);
            g.lineTo(rlb.x, rlb.y);
            g.lineTo(rlf.x, rlf.y);
            g.lineTo(llf.x, llf.y);
            g.endFill();

            // fill in the three walls
            g.beginFill(0x666666);
            g.moveTo(luf.x, luf.y);
            g.lineTo(lub.x, lub.y);
            g.lineTo(rub.x, rub.y);
            g.lineTo(ruf.x, ruf.y);
            g.lineTo(rlf.x, rlf.y);
            g.lineTo(rlb.x, rlb.y);
            g.lineTo(llb.x, llb.y);
            g.lineTo(llf.x, llf.y);
            g.lineTo(luf.x, luf.y);
            g.endFill();

            // fill in the ceiling
            g.beginFill(0x999999);
            g.moveTo(luf.x, luf.y);
            g.lineTo(lub.x, lub.y);
            g.lineTo(rub.x, rub.y);
            g.lineTo(ruf.x, ruf.y);
            g.lineTo(luf.x, luf.y);
            g.endFill();

        } else {
            g.beginFill(0xFFFFFF);
            g.drawRect(luf.x, luf.y, rlf.x - luf.x, rlf.y - luf.y);
            g.endFill();
        }

        // draw the lines defining the walls
        if (drawEdges) {
            g.lineStyle(2);
            g.moveTo(luf.x, luf.y);
            g.lineTo(lub.x, lub.y);
            g.lineTo(rub.x, rub.y);

            g.moveTo(ruf.x, ruf.y);
            g.lineTo(rub.x, rub.y);
            g.lineTo(rlb.x, rlb.y);

            g.moveTo(rlf.x, rlf.y);
            g.lineTo(rlb.x, rlb.y);
            g.lineTo(llb.x, llb.y);

            g.moveTo(llf.x, llf.y);
            g.lineTo(llb.x, llb.y);
            g.lineTo(lub.x, lub.y);

            g.lineStyle(0, 0, 0); // stop drawing lines
        }
    }

    /** The RoomMetrics for doing our layout. */
    protected var _metrics :RoomMetrics = new RoomMetrics();

    /** One of the Decor.* enum values that describe different room types. */
    protected var _roomType :int;
}
}
