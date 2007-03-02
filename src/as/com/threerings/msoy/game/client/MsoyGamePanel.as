//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.client.EZGamePanel;
import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.ChatOverlay;

import com.threerings.msoy.game.data.MsoyGameObject;


public class MsoyGamePanel extends EZGamePanel
{
    public function MsoyGamePanel (ctx :WorldContext, ctrl :MsoyGameController)
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

    override protected function createBackend () :GameControlBackend
    {
        return new WhirledGameControlBackend(
            (_ctx as WorldContext), (_ezObj as MsoyGameObject), (_ctrl as MsoyGameController));
    }

    /** Overlays chat on top of the game.
     * TODO: Use an overlay if the game is too big, otherwise
     * place a nice chatbox under the game. */
    protected var _chatOverlay :ChatOverlay;
}
}
