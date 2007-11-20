//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;

import mx.events.ResizeEvent;

import com.threerings.flash.MediaContainer;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.ezgame.client.GameControlBackend;

import com.whirled.client.PlayerList;
import com.whirled.client.WhirledGamePanel;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.HistoryList;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.game.data.MsoyGameObject;

public class MsoyGamePanel extends WhirledGamePanel
    implements MsoyPlaceView
{
    public function MsoyGamePanel (ctx :GameContext, ctrl :MsoyGameController)
    {
        super(ctx, ctrl);
        _gctx = ctx;
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

    // from EZGamePanel
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _gctx.getMsoyChatDirector().displayGameChat(_gctx.getChatDirector(), _playerList);

        var bar :ControlBar = _gctx.getTopPanel().getControlBar();
        bar.addCustomComponent(_backToLobby);
        bar.addCustomComponent(_backToWhirled);
        bar.addCustomComponent(_rematch);
    }

    // from EZGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _gctx.getMsoyChatDirector().clearGameChat();
        var bar :ControlBar = _gctx.getTopPanel().getControlBar();
        bar.setChatEnabled(true);

        _backToLobby.parent.removeChild(_backToLobby);
        _backToWhirled.parent.removeChild(_backToWhirled);
        _rematch.parent.removeChild(_rematch);
    }

    // from EZGamePanel
    override protected function createBackend () :GameControlBackend
    {
        return new MsoyGameControlBackend(_gctx, _ezObj as MsoyGameObject, 
                                          _ctrl as MsoyGameController);
    }

    // from WhirledGamePanel
    override protected function getButtonLabels () :Array
    {
        return [ Msgs.GAME.get("b.backToLobby"), Msgs.GAME.get("b.backToWhirled"),
            Msgs.GAME.get("b.rematch") ];
    }

    /** convenience reference to our game context */
    protected var _gctx :GameContext;
}
}
