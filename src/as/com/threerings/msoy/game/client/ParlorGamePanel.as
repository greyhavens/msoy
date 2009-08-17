//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.events.KeyboardEvent;
import flash.ui.Mouse;
import mx.containers.VBox;

import com.threerings.media.MediaContainer;
import com.threerings.flex.CommandButton;
import com.threerings.util.MessageBundle;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.parlor.game.data.GameConfig;

import com.whirled.game.client.GamePlayerList;
import com.whirled.game.client.WhirledGamePanel;

import com.threerings.msoy.ui.DataPackMediaContainer;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyNameLabelCreator;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.GameChatContainer;

import com.threerings.msoy.notify.data.Notification;

import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.game.data.ParlorGameObject;
import com.threerings.msoy.game.util.GameUtil;

/**
 * Coordinates the view for MSOY games.
 */
public class ParlorGamePanel extends WhirledGamePanel
    implements MsoyPlaceView
{
    // TEMP
    public static const GAMESTUB_DEBUG_MODE :Boolean = false;

    // TEMP
    public static const USE_GAMEOVER_POPUP :Boolean = false;

    public function ParlorGamePanel (gctx :GameContext, ctrl :ParlorGameController)
    {
        super(gctx, ctrl);
        _gctx = gctx;

        const cfg :ParlorGameConfig = ctrl.getPlaceConfig() as ParlorGameConfig;
        if (cfg.groupId != GameUtil.NO_GROUP) {
            _goBtn = new CommandButton(Msgs.GAME.get("b.game_whirled"),
                MsoyController.GO_GROUP_HOME, cfg.groupId);
        } else {
            _goBtn = new CommandButton(Msgs.GENERAL.get("b.back"), MsoyController.MOVE_BACK);
        }
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
    public function shouldUseChatOverlay () :Boolean
    {
        return (_showPlayers != null);
    }

    // from MsoyPlaceView
    public function getPlaceName () :String
    {
        return (_ctrl.getPlaceConfig() as ParlorGameConfig).game.name;
    }

    // from MsoyPlaceView
    public function getPlaceLogo () :MediaDesc
    {
        return (_ctrl.getPlaceConfig() as ParlorGameConfig).game.thumbMedia;
    }

    /**
     * Shuttle the specified values to the GameOverPanel, if it exists.
     */
    public function displayGameOverCoinAward (
        forReal :Boolean, coins :int, hasCookie :Boolean) :void
    {
        if (USE_GAMEOVER_POPUP) {
            createGameOverPanel();
            _gameOverPanel.displayCoinAward(forReal, coins, hasCookie);
            // but do not yet open the panel...
        }
    }

    // from WhirledGamePanel
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        var config :ParlorGameConfig = _ctrl.getPlaceConfig() as ParlorGameConfig;

        _spinner = new GameLoadingDisplay(
            _gctx.getWorldContext().getTopPanel().getPlaceContainer(),
            SplashPlaceView.getLoadingMedia(config.getSplash(), config.getThumbnail()));

        super.willEnterPlace(plobj);

        const mctx :MsoyContext = _gctx.getWorldContext();
        const bar :ControlBar = mctx.getControlBar();
        const gameChatDir :ChatDirector = _gctx.getChatDirector();
        const multiplayer :Boolean =
            config.getMatchType() == GameConfig.PARTY || config.players.length > 1;

        mctx.getUIState().setInGame(true, multiplayer);
        bar.setGameButtonIcon(getPlaceLogo());

        // if we're too small to display chat in a sidebar, do an overlay instead
        if (GAMESTUB_DEBUG_MODE || (mctx.getWidth() < TopPanel.RIGHT_SIDEBAR_WIDTH + GAME_WIDTH)) {
            // set up a button to pop/hide the _playerList
            _showPlayers = new CommandButton();
            _showPlayers.toggle = true;
            _showPlayers.toolTip = Msgs.GAME.get("i.scores");
            _showPlayers.styleName = "controlBarButtonScores";
            _showPlayers.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
                // TODO: create a class for this puppy?
                var panel :FloatingPanel = new FloatingPanel(mctx, Msgs.GAME.get("t.players"));
                panel.showCloseButton = true;
                panel.setStyle("paddingLeft", 0);
                panel.setStyle("paddingTop", 0);
                panel.setStyle("paddingRight", 0);
                panel.setStyle("paddingBottom", 0);
                var box :VBox = new VBox();
                box.setStyle("backgroundColor", 0x000000);
                box.addChild(_playerList);
                panel.addChild(box);
                return panel;
            }, _showPlayers));
            // March 16, 2009: don't add the players button for single-player games
            if (multiplayer) {
                bar.addCustomButton(_showPlayers);
            }

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            overlay.setSuppressSidebar(true);
            overlay.setLocalType(null);
            gameChatDir.addChatDisplay(overlay);

        } else {
            mctx.getTopPanel().setLeftPanel(new GameChatContainer(mctx, gameChatDir, _playerList));
        }

//        // if we're the first person in a party game, create a twittering link
//        if ((config.getMatchType() == GameConfig.PARTY) && (plobj.occupants.size() == 1)) {
//            mctx.getNotificationDirector().addGenericNotification(
//                MessageBundle.tcompose("m.tweet_game", config.getGameId(), config.game.name),
//                Notification.SYSTEM);
//        }
    }

    // from WhirledGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        const mctx :MsoyContext = _gctx.getWorldContext();

        mctx.getUIState().setInGame(false, false);

        if (_showPlayers != null) { // indicates we're in "gamestub" mode where chat is an overlay
            if (_showPlayers.parent != null) {
                _showPlayers.parent.removeChild(_showPlayers);
            }

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            overlay.setSuppressSidebar(false);
            _gctx.getChatDirector().removeChatDisplay(overlay);
//            mctx.getTopPanel().getHeaderBar().getChatTabs().locationName = null;

        } else {
            var gameChat :GameChatContainer =
                mctx.getTopPanel().getLeftPanel() as GameChatContainer;
            if (gameChat != null) {
                gameChat.shutdown();
                mctx.getTopPanel().clearLeftPanel();
            }
        }

        displayGameOver(false); // shut down any gameover display

        // TODO: shutdown _spinner?

        Mouse.show(); // re-show the mouse, in case the game hid it
    }

    // from WhirledGamePanel
    override protected function initiateLoading () :void
    {
        super.initiateLoading();

    }

    // from WhirledGamePanel
    override protected function createPlayerList () :GamePlayerList
    {
        return new MsoyGamePlayerList(
            new MsoyNameLabelCreator((_ctx as GameContext).getWorldContext()));
    }

    // from WhirledGamePanel
    override protected function getRematchLabel (plobj :PlaceObject) :String
    {
        const gameObj :ParlorGameObject = plobj as ParlorGameObject;
        return Msgs.GAME.get((gameObj.players.length == 1) ? "b.replay" : "b.rematch");
    }

    override protected function displayGameOver (gameOver :Boolean) :void
    {
        if (USE_GAMEOVER_POPUP) {
            if (gameOver) {
                createGameOverPanel();
                if (!_gameOverPanel.isOpen()) {
                    _gameOverPanel.open();
                }

            } else if (_gameOverPanel != null) {
                _gameOverPanel.close();
                _gameOverPanel = null;
            }
        }

        if (gameOver) {
            const config :ParlorGameConfig = _ctrl.getPlaceConfig() as ParlorGameConfig;
            const multiplayer :Boolean =
                config.getMatchType() == GameConfig.PARTY || config.players.length > 1;
            if (!multiplayer) {
                _gctx.getWorldContext().getUpsellDirector().noteGameOver();
            }
        }
    }

    protected function createGameOverPanel () :void
    {
        if (_gameOverPanel == null) {
            _gameOverPanel = new GameOverPanel(_gctx, _rematch, _goBtn);
        }
    }

    override protected function createGameContainer () :MediaContainer
    {
        var dpmc :DataPackMediaContainer = new DataPackMediaContainer();
        dpmc.addEventListener(DataPackMediaContainer.LOADING_MEDIA, handleLoadingGameMedia);
        return dpmc;
    }

    /**
     * Called when the actual underlying game media begins loading.
     */
    protected function handleLoadingGameMedia (event :ValueEvent) :void
    {
        var info :LoaderInfo = event.value as LoaderInfo;
        _spinner.watchLoader(info, _gameContainer, true);

        // install a listener on the MediaStub to un-idle the user when they generate key events
        // therein
        Loader(_gameContainer.getMedia()).content.addEventListener(
            KeyboardEvent.KEY_DOWN, _gctx.getWorldContext().getMsoyController().resetIdleTracking,
            false, int.MAX_VALUE, true);
    }

    /** convenience reference to our game context */
    protected var _gctx :GameContext;

    /** The loading display. */
    protected var _spinner :GameLoadingDisplay;

    /** The game over panel, or null if not being shown. */
    protected var _gameOverPanel :GameOverPanel;

    protected var _showPlayers :CommandButton;
    protected var _goBtn :CommandButton;
}
}
