//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;

import flash.ui.Mouse;

import mx.containers.VBox;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.flex.CommandButton;

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
import com.threerings.msoy.client.TopPanel;

import com.threerings.msoy.game.data.MsoyGameConfig;

import com.threerings.msoy.item.data.all.Game;

/**
 * Coordinates the view for MSOY games.
 */
public class MsoyGamePanel extends WhirledGamePanel
    implements MsoyPlaceView
{
    // TEMP
    public static const GAMESTUB_DEBUG_MODE :Boolean = false;

    // TEMP
    public static const USE_GAMEOVER_POPUP :Boolean = false;

    public function MsoyGamePanel (gctx :GameContext, ctrl :MsoyGameController)
    {
        super(gctx, ctrl);
        _gctx = gctx;

        const cfg :MsoyGameConfig = ctrl.getPlaceConfig() as MsoyGameConfig;
        if (cfg.groupId != Game.NO_GROUP) {
            _goBtn = new CommandButton(Msgs.GAME.get("b.gameWhirled"),
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
    public function padVertical () :Boolean
    {
        return false;
    }

    // from MsoyPlaceView
    public function shouldUseChatOverlay () :Boolean
    {
        return (_showPlayers != null);
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
        _spinner = new PlaceLoadingDisplay(
            _gctx.getMsoyContext().getTopPanel().getPlaceContainer());

        super.willEnterPlace(plobj);

        const mctx :MsoyContext = _gctx.getMsoyContext();
        const bar :ControlBar = mctx.getTopPanel().getControlBar();
        const gameChatDir :ChatDirector = _gctx.getChatDirector();

        mctx.getMsoyController().addGoMenuProvider(populateGoMenu);
        bar.setInGame(true);

        // if we're embedded and too small to display chat in a sidebar,
        // we go into "gamestub" mode and do an overlay instead.
        if (GAMESTUB_DEBUG_MODE ||
                (mctx.getMsoyClient().isEmbedded() &&
                mctx.getWidth() < TopPanel.RIGHT_SIDEBAR_WIDTH + GAME_WIDTH)) {
            // set up a button to pop/hide the _playerList
            _showPlayers = new CommandButton();
            _showPlayers.toolTip = Msgs.GAME.get("i.scores");
            _showPlayers.styleName = "controlBarButtonScores";
            _showPlayers.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
                // TODO: create a class for this puppy?
                var panel :FloatingPanel = new FloatingPanel(mctx, Msgs.GAME.get("t.players"));
                panel.showCloseButton = true;
                panel.styleName = "sexyWindow";
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
            bar.addCustomButton(_showPlayers);

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            overlay.setSuppressSidebar(true);
            overlay.setLocalType(null);
            gameChatDir.addChatDisplay(overlay);
            bar.setChatDirector(gameChatDir);

        } else {
            // put game chat in the sidebar
            mctx.getTopPanel().setLeftPanel(new GameChatContainer(mctx, gameChatDir, _playerList));
        }
    }

    // from WhirledGamePanel
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        const mctx :MsoyContext = _gctx.getMsoyContext();
        const bar :ControlBar = mctx.getTopPanel().getControlBar();

        bar.setInGame(false);
        mctx.getMsoyController().removeGoMenuProvider(populateGoMenu);

        if (_showPlayers != null) { // indicates we're in "gamestub" mode where chat is an overlay
            _showPlayers.parent.removeChild(_showPlayers);

            var overlay :ChatOverlay = mctx.getTopPanel().getPlaceChatOverlay();
            overlay.setSuppressSidebar(false);
            _gctx.getChatDirector().removeChatDisplay(overlay);
            bar.setChatDirector(mctx.getMsoyChatDirector());
            mctx.getTopPanel().getHeaderBar().getChatTabs().locationName = null;

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
        _spinner.watchLoader(
            Loader(_gameView.getMediaContainer().getMedia()).contentLoaderInfo,
            _gameView.getMediaContainer(), true);

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
        const cfg :MsoyGameConfig = _ctrl.getPlaceConfig() as MsoyGameConfig;

        menuData.push({ type: "separator" });
        menuData.push({ label: Msgs.GAME.get("b.allGames"), command: MsoyController.VIEW_GAMES });
        menuData.push({ label: Msgs.GAME.get("b.backToLobby"),
            callback: _gctx.backToWhirled, arg: true });
//        menuData.push({ label: Msgs.GAME.get("b.shop") /* TODO */ });
        if (cfg.groupId != Game.NO_GROUP) {
            menuData.push({ label: Msgs.GAME.get("b.gameWhirled"),
                command: MsoyController.GO_GROUP_HOME, arg: cfg.groupId });
        }
        menuData.push({ type: "separator" });
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

        } else {
            if (gameOver == (_rematch.parent != null)) {
                return;
            }
            if (gameOver) {
                const bar :ControlBar = _gctx.getMsoyContext().getTopPanel().getControlBar();
                bar.addCustomComponent(_rematch);
                bar.addCustomComponent(_goBtn);

            } else {
                _rematch.parent.removeChild(_rematch);
                _goBtn.parent.removeChild(_goBtn);
            }
        }
    }

    protected function createGameOverPanel () :void
    {
        if (_gameOverPanel == null) {
            _gameOverPanel = new GameOverPanel(_gctx, _rematch, _goBtn);
        }
    }

    /** convenience reference to our game context */
    protected var _gctx :GameContext;

    /** The loading display. */
    protected var _spinner :PlaceLoadingDisplay;

    protected var _showPlayers :CommandButton;

    protected var _goBtn :CommandButton;

    /** The game over panel, or null if not being shown. */
    protected var _gameOverPanel :GameOverPanel;
}
}
