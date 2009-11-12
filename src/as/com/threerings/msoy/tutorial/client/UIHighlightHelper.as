//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.DisplayObject;
import flash.display.MovieClip;
import flash.events.Event;
import flash.geom.Point;

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

    protected function handleEnterFrame (evt :Event) :void
    {
        var comp :IFlexDisplayObject = getComp();
        var show :Boolean = comp != null && comp.stage != null && comp.visible;

        if (show) {
            var tl :Point = toTop(comp, 0, 0);
            var br :Point = toTop(comp, comp.width, comp.height);
            var w :int = br.x - tl.x + 30;
            var h :int = br.y - tl.y + 30;

            _highlight.x = tl.x - 8;
            _highlight.y = tl.y - 10;

            if (_highlight.parent == null || comp != _lastComp) {
                _circle.width = w;
                _circle.height = h;
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
    protected var _up :Boolean;

    [Embed(source="../../../../../../../rsrc/media/skins/tutorial/circle.swf",
           mimeType="application/octet-stream")]
    protected static const CIRCLE :Class;
}
}
