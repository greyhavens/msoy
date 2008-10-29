//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.Grid;
import mx.containers.VBox;

import mx.core.UIComponent;

import com.threerings.util.MessageBundle;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.whirled.game.data.WhirledGameCodes;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * Displayed when the game is over in a non-party game.
 */
public class GameOverPanel extends FloatingPanel
{
    public function GameOverPanel (gctx :GameContext, rematch :UIComponent, goBtn :UIComponent)
    {
        super(gctx.getMsoyContext(), Msgs.GAME.get("t.gameOver"));
        _gctx = gctx;
        _rematch = rematch;
        _goBtn = goBtn;

        _topArea.setStyle("horizontalAlign", "center");
    }

    /**
     * Add additional information.
     */
    public function displayCoinAward (forReal :Boolean, coins :int, hasCookie :Boolean) :void
    {
        // I think we want to not show this shit if it's not for real..
        if (!forReal) {
            return;
        }

        if (coins > 0) {
            // TODO: the return of the coins image
            const wgMsgs :MessageBundle = _ctx.getMessageManager().getBundle(
                WhirledGameCodes.WHIRLEDGAME_MESSAGE_BUNDLE);
            _topArea.addChild(
                FlexUtil.createLabel(wgMsgs.get("m.coins_awarded", coins), "gameOverCoins"));

            var msg :String;
            if (_gctx.getPlayerObject().isGuest()) {
                msg = hasCookie ? "l.guest_flowprog_note" : "l.guest_flow_note";
            } else {
                msg = "m.goShopping";
            }
            _topArea.addChild(FlexUtil.createLabel(Msgs.GAME.get(msg), "gameOverMessage"));
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_topArea);
        //setStyle("horizontalAlign", "center");

        if (_gctx.getPlayerObject().isGuest()) {
            const signUpBtn :CommandButton = new CommandButton(null, MsoyController.SHOW_SIGN_UP);
            signUpBtn.styleName = "joinNowButton";
            addChild(signUpBtn);
        }

        const allGamesBtn :CommandButton = new CommandButton(
            Msgs.GAME.get("b.allGames"), MsoyController.VIEW_GAMES);
        const lobbyBtn :CommandButton = new CommandButton(
            Msgs.GAME.get("b.backToLobby"), _gctx.backToWhirled, true);

        _rematch.styleName = "largeBlueButton";
        allGamesBtn.styleName = "largeBlueButton";
        lobbyBtn.styleName = "largeBlueButton";
        _goBtn.styleName = "largeBlueButton";

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, _rematch, allGamesBtn);
        GridUtil.addRow(grid, lobbyBtn, _goBtn);
        addChild(grid);
    }

    /** A snakeskin filled with our own piss. */
    protected var _gctx :GameContext;

    /** The rematch thing. */
    protected var _rematch :UIComponent;

    protected var _goBtn :UIComponent;

    /** An area where custom messages may be added after the dialog is up. */
    protected var _topArea :VBox = new VBox();
}
}
