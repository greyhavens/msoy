//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;

import mx.events.ResizeEvent;

import com.threerings.flash.MediaContainer;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.ezgame.client.EZGamePanel;
import com.threerings.ezgame.client.GameControlBackend;

import com.whirled.client.PlayerList;
import com.whirled.client.WhirledGamePanel;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.HistoryList;
import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.game.data.MsoyGameObject;

public class MsoyGamePanel extends EZGamePanel
    implements WhirledGamePanel, MsoyPlaceView
{
    public function MsoyGamePanel (ctx :GameContext, ctrl :MsoyGameController)
    {
        super(ctx, ctrl);

        _playerList = new PlayerList();
    }

    // from MsoyPlaceView
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        // don't care
    }

    // from MsoyPlaceView
    public function setIsShowing (showing :Boolean) :void
    {
        // don't care
    }

    // from MsoyPlaceView
    public function usurpsControlBar () :Boolean
    {
        // NOTE: this used to usurp the control bar but the artists didn't like it; I'm leaving it
        // in here for now in case we change our mind once everyone sees what I saw when I first
        // decided to add the usurpage
        return false;
    }

    // from EZGamePanel
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        ((_ctx as GameContext).getWorldContext().getChatDirector() as MsoyChatDirector).
            displayGameChat(_ctx.getChatDirector(), _playerList);

        _playerList.startup(plobj);
    }

    // from EZGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        ((_ctx as GameContext).getWorldContext().getChatDirector() as MsoyChatDirector).
            clearGameChat();
        (_ctx as GameContext).getTopPanel().getControlBar().setChatEnabled(true);

        _playerList.shutdown();
    }

    // from WhirledGamePanel
    public function getPlayerList () :PlayerList
    {
        return _playerList;
    }

    // from EZGamePanel
    override protected function createBackend () :GameControlBackend
    {
        return new MsoyGameControlBackend(
            (_ctx as GameContext), (_ezObj as MsoyGameObject), (_ctrl as MsoyGameController));
    }

    /** The standard list of players. */
    protected var _playerList :PlayerList;
}
}
