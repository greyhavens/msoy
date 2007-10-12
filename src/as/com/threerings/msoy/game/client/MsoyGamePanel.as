//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.client.EZGamePanel;
import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.HistoryList;
import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.game.data.MsoyGameObject;

import com.threerings.flash.MediaContainer;

import com.threerings.util.ValueEvent;

import mx.events.ResizeEvent;

public class MsoyGamePanel extends EZGamePanel
{
    public function MsoyGamePanel (ctx :GameContext, ctrl :MsoyGameController)
    {
        super(ctx, ctrl);
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        ((_ctx as GameContext).getWorldContext().getChatDirector() as MsoyChatDirector).
            displayGameChat(_ctx.getChatDirector());
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        ((_ctx as GameContext).getWorldContext().getChatDirector() as MsoyChatDirector).
            clearGameChat();
        (_ctx as GameContext).getTopPanel().getControlBar().setChatEnabled(true);
    }

    override protected function createBackend () :GameControlBackend
    {
        return new MsoyGameControlBackend(
            (_ctx as GameContext), (_ezObj as MsoyGameObject), (_ctrl as MsoyGameController));
    }
}
}
