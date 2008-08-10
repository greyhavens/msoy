//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;

import mx.containers.VBox;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.flex.CommandCheckBox;

import com.whirled.game.client.GamePlayerList;
import com.whirled.game.client.WhirledGamePanel;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyNameLabelCreator;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.GameChatContainer;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.MsoyPlaceView;

import com.threerings.msoy.game.data.MsoyGameConfig;

public class MsoyGamePanel extends WhirledGamePanel
    implements MsoyPlaceView
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

    // from MsoyPlaceView
    public function shouldUseChatOverlay () :Boolean
    {
        return GAMESTUB_DEBUG_MODE;
    }

    // from MsoyPlaceView
    public function getPlaceName () :String
    {
        return (_ctrl.getPlaceConfig() as MsoyGameConfig).name;
    }

    // from MsoyPlaceView
    public function getPlaceLogo () :MediaDesc
    {
        return (_ctrl.getPlaceConfig() as MsoyGameConfig).thumbnail;
    }

    // from WhirledGamePanel
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        const mctx :MsoyContext = _gctx.getMsoyContext();
        mctx.getMsoyController().addGoMenuProvider(populateGoMenu);

        var spinner :PlaceLoadingDisplay = new PlaceLoadingDisplay(
            mctx.getTopPanel().getPlaceContainer());
        spinner.watchLoader(
            Loader(_gameView.getMediaContainer().getMedia()).contentLoaderInfo,
            _gameView.getMediaContainer(), true);

        const bar :ControlBar = mctx.getTopPanel().getControlBar();
        const gameChatDir :ChatDirector = _gctx.getChatDirector();

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
            gameChatDir.addChatDisplay(overlay);
            bar.setChatDirector(gameChatDir);

        } else {
            // put game chat in the sidebar
            mctx.getTopPanel().setRightPanel(new GameChatContainer(mctx, gameChatDir, _playerList));
        }

        bar.addCustomComponent(_rematch);
    }

    // from WhirledGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        const mctx :MsoyContext = _gctx.getMsoyContext();
        const bar :ControlBar = mctx.getTopPanel().getControlBar();

        mctx.getMsoyController().removeGoMenuProvider(populateGoMenu);

        if (GAMESTUB_DEBUG_MODE) {
            _showPlayers.parent.removeChild(_showPlayers);

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            _gctx.getChatDirector().removeChatDisplay(overlay);
            bar.setChatDirector(mctx.getMsoyChatDirector());
            mctx.getTopPanel().getHeaderBar().getChatTabs().locationName = null;

        } else {
            var gameChat :GameChatContainer =
                mctx.getTopPanel().getRightPanel() as GameChatContainer;
            if (gameChat != null) {
                gameChat.shutdown();
                mctx.getTopPanel().clearRightPanel();
            }
        }

        // clear our custom controlbar components
        _rematch.parent.removeChild(_rematch);
    }

    // from WhirledGamePanel
    override protected function createPlayerList () :GamePlayerList
    {
        return new MsoyGamePlayerList(
            new MsoyNameLabelCreator((_ctx as GameContext).getMsoyContext()));
    }

    // from WhirledGamePanel
    override protected function getRematchLabel (plobj :PlaceObject) :String
    {
        const gameObj :WhirledGameObject = plobj as WhirledGameObject;
        return Msgs.GAME.get((gameObj.players.length == 1) ? "b.replay" : "b.rematch");
    }

    /**
     * Populates any game-specific entries on the client's "go" menu.
     */
    protected function populateGoMenu (menuData :Array) :void
    {
        menuData.push({ type: "separator" });
        menuData.push({ label: Msgs.GAME.get("b.allGames"), command: MsoyController.VIEW_GAMES });
        menuData.push({ label: Msgs.GAME.get("b.backToLobby"),
            callback: _gctx.backToWhirled, arg: true });
//        menuData.push({ label: Msgs.GAME.get("b.shop") /* TODO */ });
//        menuData.push({ label: Msgs.GAME.get("b.gameWhirled") /* TODO */ });
        menuData.push({ type: "separator" });
    }

    /** convenience reference to our game context */
    protected var _gctx :GameContext;

    protected var _showPlayers :CommandCheckBox;
}
}
