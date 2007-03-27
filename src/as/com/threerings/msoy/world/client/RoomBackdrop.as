//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Graphics;
import flash.display.Sprite;

import com.threerings.msoy.item.web.Decor;
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
     * Draws the room backdrop on the specified sprite, with specified
     * unscaled parameters.
     */
    public function drawRoom (
        sprite :Sprite, unscaledWidth :Number, unscaledHeight :Number, editing :Boolean) :void
    {
        var g :Graphics = sprite.graphics;
        g.clear();

        // fill all our screen area with transparent pixels, so that
        // mousing anywhere in our bounds includes us in the
        // event dispatch. This is primarily necessary to get the
        // ContextMenu working properly.
        if (!isNaN(unscaledWidth) && !isNaN(unscaledHeight)) {
            var w :Number = unscaledWidth / sprite.scaleX;
            var h :Number = unscaledHeight / sprite.scaleY;
            g.beginFill(0, 0);
            g.drawRect(0, 0, w, h);
            g.endFill();
        }
 
        var drawWalls :Boolean = (_roomType == Decor.DRAWN_ROOM);
        var drawEdges :Boolean = drawWalls || editing;
        if (!drawEdges) {
            return; // nothing to draw
        }

        var floorWidth :Number = (_metrics.sceneWidth * _metrics.minScale);
        var floorInset :Number = (_metrics.sceneWidth - floorWidth) / 2;

        // rename a few things for ease of use below...
        var x1 :Number = floorInset;
        var x2 :Number = _metrics.sceneWidth - floorInset;
        var y1 :Number = _metrics.backWallTop;
        var y2 :Number = _metrics.backWallBottom;

        if (drawWalls) {
            // fill in the floor
            g.beginFill(0x333333);
            g.moveTo(0, _metrics.sceneHeight);
            g.lineTo(x1, y2);
            g.lineTo(x2, y2);
            g.lineTo(_metrics.sceneWidth, _metrics.sceneHeight);
            g.lineTo(0, _metrics.sceneHeight);
            g.endFill();

            // fill in the three walls
            g.beginFill(0x666666);
            g.moveTo(0, 0);
            g.lineTo(x1, y1);
            g.lineTo(x2, y1);
            g.lineTo(_metrics.sceneWidth, 0);
            g.lineTo(_metrics.sceneWidth, _metrics.sceneHeight);
            g.lineTo(x2, y2);
            g.lineTo(x1, y2);
            g.lineTo(0, _metrics.sceneHeight);
            g.lineTo(0, 0);
            g.endFill();

            // fill in the ceiling
            g.beginFill(0x999999);
            g.moveTo(0, 0);
            g.lineTo(x1, y1);
            g.lineTo(x2, y1);
            g.lineTo(_metrics.sceneWidth, 0);
            g.lineTo(0, 0);
            g.endFill();

        } else {
            g.beginFill(0xFFFFFF);
            g.drawRect(0, 0, _metrics.sceneWidth, _metrics.sceneHeight);
            g.endFill();
        }

        // draw the lines defining the walls
        if (drawEdges) {
            g.lineStyle(2);
            g.moveTo(0, 0);
            g.lineTo(x1, y1);
            g.lineTo(x2, y1);

            g.moveTo(_metrics.sceneWidth, 0);
            g.lineTo(x2, y1);
            g.lineTo(x2, y2);

            g.moveTo(_metrics.sceneWidth, _metrics.sceneHeight);
            g.lineTo(x2, y2);
            g.lineTo(x1, y2);

            g.moveTo(0, _metrics.sceneHeight);
            g.lineTo(x1, y2);
            g.lineTo(x1, y1);

            g.lineStyle(0, 0, 0); // stop drawing lines
        }
    }

    /** The RoomMetrics for doing our layout. */
    protected var _metrics :RoomMetrics = new RoomMetrics();

    /** One of the Decor.* enum values that describe different room types. */
    protected var _roomType :int;
}

}
