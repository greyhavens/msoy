//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;

import com.threerings.crowd.data.PlaceObject;
import com.whirled.game.client.GameBackend;

import com.whirled.game.client.WhirledGamePanel;
import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.game.data.MsoyGameConfig;

import com.threerings.msoy.world.client.WorldContext;

public class MsoyGamePanel extends WhirledGamePanel
    implements MsoyPlaceView
{
    public function MsoyGamePanel (gctx :GameContext, ctrl :MsoyGameController)
    {
        super(gctx, ctrl);
        _gctx = gctx;
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
    public function padVertical () :Boolean
    {
        return false;
    }

    // from MsoyPlaceView
    public function getChatOverlay () :ChatOverlay
    {
        return null;
    }

    // from MsoyPlaceView
    public function setUseChatOverlay (useOverlay :Boolean) :void
    {
        // nada
    }

    // from WhirledGamePanel
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        var spinner :PlaceLoadingDisplay = new PlaceLoadingDisplay(
            _gctx.getMsoyContext().getTopPanel().getPlaceContainer());
        spinner.watchLoader(
            Loader(_gameView.getMediaContainer().getMedia()).contentLoaderInfo, true);

        _gctx.getMsoyContext().getMsoyChatDirector().displayGameChat(
            _gctx.getChatDirector(), _playerList);

        var bar :ControlBar = _gctx.getMsoyContext().getTopPanel().getControlBar();
        bar.addCustomComponent(_rematch);
        bar.addCustomComponent(_backToLobby);
        bar.addCustomComponent(_backToWhirled);
    }

    // from WhirledGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _gctx.getMsoyContext().getMsoyChatDirector().clearGameChat();

        // clear our custom controlbar components
        _rematch.parent.removeChild(_rematch);
        _backToLobby.parent.removeChild(_backToLobby);
        _backToWhirled.parent.removeChild(_backToWhirled);
    }

    // from WhirledGamePanel
    override protected function createBackend () :GameBackend
    {
        return new MsoyGameBackend(_gctx, _gameObj, _ctrl as MsoyGameController);
    }

    // from WhirledGamePanel
    override protected function getButtonLabels (plobj :PlaceObject) :Array
    {
        var gameObj :WhirledGameObject = plobj as WhirledGameObject;
        return [
            Msgs.GAME.get("b.backToWhirled"),
            Msgs.GAME.get("b.backToLobby", (_ctrl.getPlaceConfig() as MsoyGameConfig).name),
            Msgs.GAME.get((gameObj.players.length == 1) ? "b.replay" : "b.rematch") ];
    }

    /** convenience reference to our game context */
    protected var _gctx :GameContext;
}
}
