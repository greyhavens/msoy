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
        // Important: we need to start the playerList prior to calling super, so that it
        // is added as a listener to the gameObject prior to the backend being created
        // and added as a listener. That way, when the ezgame hears about an occupantAdded
        // event, the playerList already knows about that player!
        _playerList.startup(plobj);

        super.willEnterPlace(plobj);

        ((_ctx as GameContext).getWorldContext().getChatDirector() as MsoyChatDirector).
            displayGameChat(_ctx.getChatDirector(), _playerList);
    }

    // from EZGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _playerList.shutdown();

        super.didLeavePlace(plobj);

        ((_ctx as GameContext).getWorldContext().getChatDirector() as MsoyChatDirector).
            clearGameChat();
        (_ctx as GameContext).getTopPanel().getControlBar().setChatEnabled(true);
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
