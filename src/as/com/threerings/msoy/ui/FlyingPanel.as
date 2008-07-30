//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import flash.events.Event;

import flash.geom.Rectangle;

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

        systemManager.addEventListener(Event.RESIZE, handleStageResized);
    }

    override public function close () :void
    {
        systemManager.removeEventListener(Event.RESIZE, handleStageResized);

        super.close();
    }

    // TODO: move this back to the friendsList?
    protected function handleStageResized (...ignored) :void
    {
        var placeBounds :Rectangle = _ctx.getTopPanel().getPlaceViewBounds();
        // fix the height
        height = placeBounds.height - PADDING * 2;
        // fit the popup within the new bounds, minux padding.
        placeBounds.x += PADDING;
        placeBounds.y += PADDING;
        placeBounds.width -= PADDING * 2;
        placeBounds.height -= PADDING * 2;
        PopUpUtil.fitInRect(this, placeBounds);
    }

    protected function handleMiniWillChange (event :ValueEvent) :void
    {
        if (event.value) {
            _currentX = x;
        } else {
            x = _currentX;
        }
    }

    /** Saves our x value across client minimizations. */
    protected var _currentX :int;

    protected static const PADDING :int = 10;
}
}
