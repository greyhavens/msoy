//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.Graphics;

import flash.geom.Point;

import com.threerings.msoy.item.data.all.Decor;

/**
 * Helper class that draws a room backdrop with four walls, floor, and ceiling.
 */
public class RoomBackdrop
{
    /** Refresh room metrics. */
    public function update (decor :Decor) :void
    {
        _decor = decor;
        _metrics.update(decor);
    }

    /**
     * Draws the room backdrop on the specified graphics, with specified unscaled parameters.
     */
    public function drawRoom (
        g :Graphics, unscaledWidth :Number, unscaledHeight :Number,
        drawEdges :Boolean, fillWalls :Boolean, edgeAlpha :Number = 1.0) :void
    {
        g.clear();

        if (_decor.type == Decor.FLAT_LAYOUT) {
            return;
        }

        // fill all our screen area with transparent pixels, so that mousing anywhere in our bounds
        // includes us in the event dispatch. This is primarily necessary to get the ContextMenu
        // working properly.
        // todo: remove?
        if (!isNaN(unscaledWidth) && !isNaN(unscaledHeight)) {
            g.beginFill(0, 0);
            g.drawRect(0, 0, unscaledWidth, unscaledHeight);
            g.endFill();
        }

        if (!drawEdges) {
            return; // nothing to draw
        }

        // helper function
        var drawLines :Function = function (points :Array /* of Point */) :void {
            var start :Point = points[0];
            g.moveTo(start.x, start.y);
            for (var ii :int = 1; ii < points.length; ii++) {
                var p :Point = points[ii];
                g.lineTo(p.x, p.y);
            }
        }                
        
        // variable names: [l/r][l/u][f/b] - left/right, lower/upper, front/back
        // Note: you can verify that a line connects two corners along an edge by checking that the
        // two endpoints differ in only one letter.
        var llf :Point = _metrics.roomToScreen(0, 0, 0);
        var llb :Point = _metrics.roomToScreen(0, 0, 1);
        var luf :Point = _metrics.roomToScreen(0, 1, 0);
        var lub :Point = _metrics.roomToScreen(0, 1, 1);

        var rlf :Point = _metrics.roomToScreen(1, 0, 0);
        var rlb :Point = _metrics.roomToScreen(1, 0, 1);
        var ruf :Point = _metrics.roomToScreen(1, 1, 0);
        var rub :Point = _metrics.roomToScreen(1, 1, 1);

        var wallFn :Function = function (width :Number, color :uint, alpha :Number) :void {
            g.lineStyle(width, color, alpha);
            drawLines([luf, lub, rub]);
            drawLines([ruf, rub, rlb]);
            drawLines([rlf, rlb, llb]);
            drawLines([llf, llb, lub]);
        }

        var floorFn :Function = function (width :Number, color :uint, alpha :Number) :void {
            g.lineStyle(width, color, alpha);
            var delta :Number = 0.2;
            for (var ii :Number = delta; ii < 1.0; ii += delta) {
                drawLines([_metrics.roomToScreen(ii, 0, 0), _metrics.roomToScreen(ii, 0, 1)]);
                drawLines([_metrics.roomToScreen(0, 0, ii), _metrics.roomToScreen(1, 0, ii)]);
            }
        }
        
        if (fillWalls) {
            // fill in the floor
            g.beginFill(0x995a01);
            drawLines([llf, llb, rlb, rlf, llf]);
            g.endFill();

            // fill in the three walls
            g.beginFill(0x195178);
            drawLines([luf, lub, rub, ruf, rlf, rlb, llb, llf, luf]);
            g.endFill();

            // fill in the ceiling
            g.beginFill(0x97c3e1);
            drawLines([luf, lub, rub, ruf, luf]);
            g.endFill();
        } 

        // draw the lines defining the walls
        if (drawEdges) {
            floorFn(1, 0xffffff, edgeAlpha);
            wallFn(3, 0x000000, edgeAlpha);
            wallFn(1, 0xffffff, edgeAlpha);
            g.lineStyle(0, 0, 0); // stop drawing lines
        }
    }

    protected var _decor :Decor;

    /** The RoomMetrics for doing our layout. */
    protected var _metrics :RoomMetrics = new RoomMetrics();
}
}
