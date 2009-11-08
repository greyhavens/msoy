//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.events.Event;
import flash.geom.Point;

import mx.core.UIComponent;

import com.threerings.msoy.client.TopPanel;

/**
 * Popup helper that highlights a ui component using a standard tutorial effect.
 */
public class UIHighlightHelper
    implements PopupHelper
{
    /**
     * Creates a new highlight helper.
     */
    public function UIHighlightHelper (topPanel :TopPanel, comp :UIComponent)
    {
        _top = topPanel;
        _comp = comp;
    }

    /** @inheritDocs */
    public function popup () :void
    {
        if (_highlight != null) {
            return;
        }

        var tl :Point = toTop(0, 0);
        var br :Point = toTop(_comp.width, _comp.height);
        _highlight = new UIComponent();

        // TODO: animation... something more eyecatching
        // TODO: update the position of the highlight each frame in case _comp moves
        _highlight.graphics.clear();
        _highlight.graphics.lineStyle(2, 0xff0000);
        _highlight.graphics.drawRect(0, 0, br.x - tl.x, br.y - tl.y);
        _highlight.visible = false;

        _top.addChild(_highlight);

        _comp.addEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    /** @inheritDocs */
    public function popdown () :void
    {
        _comp.removeEventListener(Event.ENTER_FRAME, handleEnterFrame);

        if (_highlight == null) {
            return;
        }
        _top.removeChild(_highlight);
        _highlight = null;
    }

    protected function handleEnterFrame (evt :Event) :void
    {
        if (_highlight != null && _comp != null) {
            _highlight.visible = _comp.stage != null;
            var tl :Point = toTop(0, 0);
            _highlight.x = tl.x;
            _highlight.y = tl.y;
        }
    }

    protected function toTop (x :Number, y :Number) :Point
    {
        return _top.globalToLocal(_comp.localToGlobal(new Point(x, y)));
    }

    protected var _top :TopPanel;
    protected var _comp :UIComponent;
    protected var _highlight :UIComponent;
}
}
