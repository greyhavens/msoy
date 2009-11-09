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
     * @param topPanel the top panel of the client
     * @param comp either a UI component or a function that returns a UI component. In the case of
     *        the former, the highlight always surrounds the fixed component. In the case of the
     *        latter, the highlight will adapt to surround whatever component is returned at the
     *        beginning of the frame and disappearing when null is returned.
     *          <listing verion="3.0">
     *              function comp () :UIComponent;
     *          </listing>
     */
    public function UIHighlightHelper (topPanel :TopPanel, comp :Object)
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

        var comp :UIComponent = getComp();
        if (comp == null) {
            return;
        }

        // TODO: animation... something more eyecatching
        _highlight = new UIComponent();
        _highlight.addEventListener(Event.ENTER_FRAME, handleEnterFrame);
        _highlight.visible = false;
        _top.addChild(_highlight);

    }

    /** @inheritDocs */
    public function popdown () :void
    {
        if (_highlight == null) {
            return;
        }

        _highlight.removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
        _top.removeChild(_highlight);
        _highlight = null;
    }

    protected function getComp () :UIComponent
    {
        if (_comp is UIComponent) {
            return UIComponent(_comp);
        } else if (_comp is Function) {
            return UIComponent((_comp as Function)());
        }
        return null;
    }

    protected function handleEnterFrame (evt :Event) :void
    {
        var comp :UIComponent = getComp();
        if (true == (_highlight.visible = (comp != null && comp.stage != null))) {
            var tl :Point = toTop(comp, 0, 0);
            var br :Point = toTop(comp, comp.width, comp.height);

            _highlight.x = tl.x;
            _highlight.y = tl.y;

            // TODO: this is really inefficient, but it is placeholder
            _highlight.graphics.clear();
            _highlight.graphics.lineStyle(2, 0xff0000);
            _highlight.graphics.drawRect(0, 0, br.x - tl.x, br.y - tl.y);
        }
    }

    protected function toTop (comp :UIComponent, x :Number, y :Number) :Point
    {
        return _top.globalToLocal(comp.localToGlobal(new Point(x, y)));
    }

    protected var _top :TopPanel;
    protected var _comp :Object;
    protected var _highlight :UIComponent;
}
}
