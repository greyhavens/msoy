//
// $Id$

package com.threerings.msoy.ui {

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

    override public function close () :void
    {
        systemManager.removeEventListener(Event.RESIZE, checkPositioning);
        removeEventListener(ResizeEvent.RESIZE, checkPositioning);

        super.close();
    }

    override protected function didOpen () :void
    {
        super.didOpen();

        systemManager.addEventListener(Event.RESIZE, checkPositioning);
        addEventListener(ResizeEvent.RESIZE, checkPositioning);
    }

    protected function checkPositioning (... ignored) :void
    {
        var placeBounds :Rectangle = _ctx.getPlaceViewBounds();
        // fit the popup within the new bounds, minus padding.
        placeBounds.inflate(-PADDING, -PADDING);

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
}
}
