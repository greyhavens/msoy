//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.DisplayObject;
import flash.display.MovieClip;
import flash.events.Event;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.core.UIComponent;
import mx.core.IFlexDisplayObject;
import mx.managers.PopUpManager;
import mx.managers.PopUpManagerChildList;

import com.threerings.util.MultiLoader;

import com.threerings.flex.FlexUtil;

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

        MultiLoader.getContents(CIRCLE, function (result :MovieClip) :void {
            _circle = result;
            _highlight = new UIComponent();
            _highlight.mouseEnabled = false;
            _highlight.addChild(_circle);
        });
    }

    /** @inheritDocs */
    public function popup () :void
    {
        if (_highlight == null || _up) {
            return;
        }

        var comp :IFlexDisplayObject = getComp();
        if (comp == null) {
            return;
        }

        _up = true;
        _highlight.addEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    /** @inheritDocs */
    public function popdown () :void
    {
        if (!_up) {
            return;
        }

        if (_highlight.parent != null) {
            PopUpManager.removePopUp(_highlight);
        }
        _highlight.removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
        _up = false;
        _lastComp = null;
    }

    protected function getComp () :IFlexDisplayObject
    {
        if (_comp is UIComponent) {
            return UIComponent(_comp);
        } else if (_comp is Function) {
            return UIComponent((_comp as Function)());
        }
        return null;
    }

    protected function getHighlightArea (comp :IFlexDisplayObject) :Rectangle
    {
        if (comp == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        var tl :Point = toTop(comp, 0, 0);
        var br :Point = toTop(comp, comp.width, comp.height);
        var area :Rectangle = new Rectangle(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
        adjustHighlightArea(comp, area);
        return area;
    }

    protected function adjustHighlightArea (comp :IFlexDisplayObject, area:Rectangle) :void
    {
        area.inflate(8, 10);
        area.width += 14;
        area.height += 10;
    }

    protected function handleEnterFrame (evt :Event) :void
    {
        var comp :IFlexDisplayObject = getComp();
        var show :Boolean = comp != null && comp.stage != null && comp.visible;
        var area :Rectangle = getHighlightArea(comp);

        if (show) {
            if (_highlight.x != area.x || _highlight.y != area.y) {
                _highlight.x = area.x;
                _highlight.y = area.y;
            }

            if (_lastArea == null || Math.abs(_lastArea.width - area.width) > 0.01 ||
                Math.abs(_lastArea.height - area.height) > 0.01) {
                var rect :Rectangle = _circle.getRect(_circle);
                _circle.scaleX = area.width / rect.width;
                _circle.scaleY = area.height / rect.height;
            }

            if (_highlight.parent == null || comp != _lastComp) {
                _circle.gotoAndPlay(0);
            }

            if (_highlight.parent == null) {
                PopUpManager.addPopUp(_highlight, _top, false, PopUpManagerChildList.POPUP);
            }

        } else {
            if (_highlight.parent != null) {
                PopUpManager.removePopUp(_highlight);
            }
        }

        _lastArea = area;
        _lastComp = comp;
    }

    protected function toTop (comp :IFlexDisplayObject, x :Number, y :Number) :Point
    {
        return _top.globalToLocal(comp.localToGlobal(new Point(x, y)));
    }

    protected var _top :TopPanel;
    protected var _comp :Object;
    protected var _circle :MovieClip;
    protected var _highlight :UIComponent;
    protected var _lastComp :IFlexDisplayObject;
    protected var _lastArea :Rectangle;
    protected var _up :Boolean;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/circle.swf",
           mimeType="application/octet-stream")]
    protected static const CIRCLE :Class;
}
}
