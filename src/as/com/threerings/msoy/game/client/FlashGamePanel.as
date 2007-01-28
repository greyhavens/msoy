//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.client.EZGamePanel;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.chat.client.ChatOverlay;


public class FlashGamePanel extends EZGamePanel
{
    public function FlashGamePanel (ctx :MsoyContext, ctrl :FlashGameController)
    {
        super(ctx, ctrl);

        // There is a bug in BoxLayout that causes children
        // that should not be included in layout to be positioned anyway.
        // So we turn off layout, which is probably a bad idea.
        autoLayout = false;
        // TODO: this panel reports a bogus size when the overlay is
        // first setting up the history bar. It notices when the panel
        // resizes if the user resizes the msoy window, but otherwise it
        // doesn't. Fix.
        _chatOverlay = new ChatOverlay(ctx);
        _chatOverlay.setSubtitlePercentage(1);
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
