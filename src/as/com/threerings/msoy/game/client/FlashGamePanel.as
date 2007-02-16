//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.client.EZGamePanel;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.ChatOverlay;


public class FlashGamePanel extends EZGamePanel
{
    public function FlashGamePanel (ctx :WorldContext, ctrl :FlashGameController)
    {
        super(ctx, ctrl);

        _chatOverlay = new ChatOverlay(ctx);
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _chatOverlay.setTarget(this);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _chatOverlay.setTarget(null);

        super.didLeavePlace(plobj);
    }

    /** Overlays chat on top of the game. */
    protected var _chatOverlay :ChatOverlay;
}
}
