//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import flash.events.Event;

import flash.geom.Rectangle;

import mx.events.ResizeEvent;

import com.threerings.util.ValueEvent;

import com.threerings.flex.PopUpUtil;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;

/**
 * Like a floating panel, only it moves itself around the screen as the client
 * grows or minimizes, so that it stays on-screen.
 */
public class FlyingPanel extends FloatingPanel
{
    public function FlyingPanel (ctx :MsoyContext, title :String = "")
    {
        super(ctx, title);

        ctx.getClient().addEventListener(MsoyClient.MINI_WILL_CHANGE, handleMiniWillChange);
    }

    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, center :Boolean = true) :void
    {
        super.open(modal, parent, center);

        systemManager.addEventListener(Event.RESIZE, checkPositioning);
        addEventListener(ResizeEvent.RESIZE, checkPositioning);
    }

    override public function close () :void
    {
        systemManager.removeEventListener(Event.RESIZE, checkPositioning);
        removeEventListener(ResizeEvent.RESIZE, checkPositioning);

        super.close();
    }

    protected function checkPositioning (... ignored) :void
    {
        var placeBounds :Rectangle = _ctx.getTopPanel().getPlaceViewBounds();
        // fit the popup within the new bounds, minux padding.
        placeBounds.x += PADDING;
        placeBounds.y += PADDING;
        placeBounds.width -= PADDING * 2;
        placeBounds.height -= PADDING * 2;

        // fix our height (Avoid using Math.min here, because it re-assigns height and
        // screws up some things during resizing..
        if (height > placeBounds.height) {
            height = placeBounds.height;
        }

        // and make sure we fit inside
        PopUpUtil.fitInRect(this, placeBounds);
    }

    protected function handleMiniWillChange (event :ValueEvent) :void
    {
        if (event.value) {
            _currentX = x;

        } else if (!isNaN(_currentX)) {
            x = _currentX;
            _currentX = NaN;
        }
    }

    /** Saves our x value across client minimizations. */
    protected var _currentX :Number = NaN;

    protected static const PADDING :int = 10;
}
}
