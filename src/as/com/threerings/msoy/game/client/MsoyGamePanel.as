//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;

import mx.containers.VBox;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.CommandCheckBox;

import com.whirled.game.client.GamePlayerList;
import com.whirled.game.client.WhirledGamePanel;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyNameLabelCreator;

import com.threerings.msoy.chat.client.ChatOverlay;

import com.threerings.msoy.client.ChatPlaceView;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyPlaceView;

import com.threerings.msoy.game.data.MsoyGameConfig;

public class MsoyGamePanel extends WhirledGamePanel
    implements MsoyPlaceView, ChatPlaceView
{
    // TEMP
    public static const GAMESTUB_DEBUG_MODE :Boolean = false;

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

    // from WhirledGamePanel
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        const mctx :MsoyContext = _gctx.getMsoyContext();

        var spinner :PlaceLoadingDisplay = new PlaceLoadingDisplay(
            mctx.getTopPanel().getPlaceContainer());
        spinner.watchLoader(
            Loader(_gameView.getMediaContainer().getMedia()).contentLoaderInfo,
            _gameView.getMediaContainer(), true);

        const bar :ControlBar = mctx.getTopPanel().getControlBar();

        if (GAMESTUB_DEBUG_MODE) {
            // set up a button to pop/hide the _playerList
            _showPlayers = new CommandCheckBox("view scores");
            _showPlayers.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
                // TODO: create a class for this puppy?
                var panel :FloatingPanel = new FloatingPanel(mctx, "Players List"); // TODO i18n
                panel.showCloseButton = true;
                var box :VBox = new VBox();
                box.setStyle("backgroundColor", 0x000000);
                box.addChild(_playerList);
                panel.addChild(box);
                return panel;
            }, _showPlayers));
            bar.addCustomComponent(_showPlayers);

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            overlay.setLocalType(null);
            _gctx.getChatDirector().addChatDisplay(overlay);
            bar.setChatDirector(_gctx.getChatDirector());

        } else {
            // put game chat in the sidebar
            _gctx.getMsoyContext().getMsoyChatDirector().displayGameChat(
                _gctx.getChatDirector(), _playerList);
        }

        bar.addCustomComponent(_rematch);
        bar.addCustomComponent(_backToLobby);
        bar.addCustomComponent(_backToWhirled);
    }

    // from WhirledGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        const mctx :MsoyContext = _gctx.getMsoyContext();
        const bar :ControlBar = mctx.getTopPanel().getControlBar();

        if (GAMESTUB_DEBUG_MODE) {
            _showPlayers.parent.removeChild(_showPlayers);

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            _gctx.getChatDirector().removeChatDisplay(overlay);
            bar.setChatDirector(mctx.getMsoyChatDirector());
            mctx.getTopPanel().getHeaderBar().getChatTabs().locationName = null;

        } else {
            mctx.getMsoyChatDirector().clearGameChat();
        }

        // clear our custom controlbar components
        _rematch.parent.removeChild(_rematch);
        _backToLobby.parent.removeChild(_backToLobby);
        _backToWhirled.parent.removeChild(_backToWhirled);
    }

    // from WhirledGamePanel
    override protected function createPlayerList () :GamePlayerList
    {
        return new MsoyGamePlayerList(
            new MsoyNameLabelCreator((_ctx as GameContext).getMsoyContext()));
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

    protected var _showPlayers :CommandCheckBox;
}
}
