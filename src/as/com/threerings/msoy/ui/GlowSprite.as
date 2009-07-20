//
// $Id$

package com.threerings.msoy.ui {

import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.filters.GlowFilter;

import com.threerings.util.CommandEvent;

/**
 * A parent that can be configured to automatically glow on mouse hover, and submit a command
 */
public class GlowSprite extends Sprite
{
    public function init (color :uint, cmd :Object, arg :Object = null) :void
    {
        _color = color;
        _cmd = cmd;
        _arg = arg;

        addEventListener(MouseEvent.CLICK, handleClick);
        addEventListener(MouseEvent.MOUSE_OVER, handleHoverOver);
        addEventListener(MouseEvent.MOUSE_OUT, handleHoverOut);
    }

    protected function handleHoverOver (... ignored) :void
    {
        // TODO: use FilterUtil for safe add?
        this.filters = [ new GlowFilter(_color, 1, 32, 32, 2) ];
    }

    protected function handleHoverOut (... ignored) :void
    {
        // TODO: use FilterUtil for safe removal
        this.filters = null;
    }

    protected function handleClick (event :MouseEvent) :void
    {
        CommandEvent.dispatch(this, _cmd, _arg);
        event.stopImmediatePropagation();
    }

    protected var _color :uint;
    protected var _cmd :Object;
    protected var _arg :Object;
}
}
