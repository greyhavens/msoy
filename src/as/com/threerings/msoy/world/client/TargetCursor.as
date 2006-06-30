package com.threerings.msoy.world.client {

import flash.display.Graphics;
import flash.display.Shape;

public class TargetCursor extends Shape
{
    public function TargetCursor ()
    {
        graphics.clear();
        graphics.lineStyle(3, 0xFFFF00);
        graphics.moveTo(0, -5);
        graphics.lineTo(0, 6);
        graphics.moveTo(-5, 0);
        graphics.lineTo(6, 0);

        graphics.lineStyle(1, 0x000000);
        graphics.moveTo(0, -4);
        graphics.lineTo(0, 5);
        graphics.moveTo(-4, 0);
        graphics.lineTo(5, 0);
    }
}
}
