//
// $Id$

package com.threerings.msoy.game.chiyogami.client {

import flash.display.GradientType;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.geom.Matrix;

public class HealthMeter extends Sprite
{
    public function HealthMeter ()
    {
        setHealth(1);
    }

    /**
     * Set the health value (between 0 and 1)
     */
    public function setHealth (health :Number) :void
    {
        var extent :Number = health * WIDTH;
        if (health > 0 && extent < MIN_ALIVE_PIXELS) {
            extent = MIN_ALIVE_PIXELS;
        }

        // get ready to draw
        var g :Graphics = graphics;
        g.clear();

        // draw a black box in our 'dead' area
        if (extent < WIDTH) {
            g.beginFill(0x000000);
            g.drawRect(extent, 0, WIDTH - extent, HEIGHT);
            g.endFill();
        }

        // draw the gradient
        if (extent > 0) {
            var gradix :Matrix = new Matrix();
            gradix.createGradientBox(WIDTH, HEIGHT);
            g.beginGradientFill(GradientType.LINEAR, [ 0xFF0000, 0x00FF00 ],
                [ 1, 1 ], [ 0, 100 ], gradix);
            g.drawRect(0, 0, extent, HEIGHT);
            g.endFill();
        }

        // draw a white border
        g.lineStyle(1, 0xFFFFFF);
        g.drawRect(0, 0, WIDTH, HEIGHT);
        g.lineStyle(0, 0, 0);
    }

    protected static const WIDTH :int = 200;
    protected static const HEIGHT :int = 14;

    /** The minimum number of pixels to show if the health is nonzero. */
    protected static const MIN_ALIVE_PIXELS :int = 2;
}
}
