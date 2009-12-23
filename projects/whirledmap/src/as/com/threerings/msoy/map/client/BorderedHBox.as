//
// $Id$

package com.threerings.msoy.map.client {

import mx.containers.HBox;

public class BorderedHBox extends HBox
{
    public function BorderedHBox (thickness :Number = 4, color :uint = 0x000000, rounding :int = 8)
    {
        _thickness = thickness;
        _rounding = rounding;
        _color = color;
    }

    override protected
    function updateDisplayList (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        this.graphics.lineStyle(_thickness, _color);
        this.graphics.beginFill(0xeeeeee, 1);
        this.graphics.drawRoundRect(0, 0, unscaledWidth, unscaledHeight, _rounding);
        this.graphics.endFill();
    }

    protected var _thickness :Number;
    protected var _rounding :Number;
    protected var _color :uint;
}
}
