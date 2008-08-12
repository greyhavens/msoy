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
    public function GameOverPanel (gctx :GameContext, rematch :UIComponent)
    {
        super(gctx.getMsoyContext(), Msgs.GAME.get("t.gameOver"));
        _gctx = gctx;
        _rematch = rematch;

        _topArea.setStyle("horizontalAlign", "center");
//        makeTrans(_topArea);
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
            this.styleName = "coinsGameOverPanel";

            const wgMsgs :MessageBundle = _ctx.getMessageManager().getBundle(
                WhirledGameCodes.WHIRLEDGAME_MESSAGE_BUNDLE);
            this.title = wgMsgs.get("m.coins_awarded", coins);

            if (_gctx.getPlayerObject().isGuest()) {
                _topArea.addChild(FlexUtil.createLabel(Msgs.GAME.get(hasCookie ?
                    "l.guest_flowprog_note" : "l.guest_flow_note")));
            } else {
                _topArea.addChild(FlexUtil.createLabel(Msgs.GAME.get("m.goShopping")));
            }
            // TODO: style all these labels!
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_topArea);

        if (_gctx.getPlayerObject().isGuest()) {
            const signUpBtn :CommandButton = new CommandButton(null, MsoyController.SHOW_SIGN_UP);
            signUpBtn.styleName = "joinNowButton";

            var box :VBox = new VBox();
            box.percentWidth = 100;
            box.setStyle("horizontalAlign", "center");
//            makeTrans(box);
            box.addChild(signUpBtn);
            addChild(box);
        }

        const allGamesBtn :CommandButton = new CommandButton(
            Msgs.GAME.get("b.allGames"), MsoyController.VIEW_GAMES);
        const lobbyBtn :CommandButton = new CommandButton(
            Msgs.GAME.get("b.backToLobby"), _gctx.backToWhirled, true);
//        const whirledBtn :CommandButton = new CommandButton(
//            Msgs.GAME.get("b.gameWhirled"), /* TODO */);

        _rematch.styleName = "blueButton";
        allGamesBtn.styleName = "blueButton";
        lobbyBtn.styleName = "blueButton";
//        whirledBtn.styleName = "blueButton";

        var grid :Grid = new Grid();
//        makeTrans(grid);
        GridUtil.addRow(grid, _rematch, allGamesBtn);
        GridUtil.addRow(grid, lobbyBtn /*, whirledBtn */);
        addChild(grid);
    }

    protected function makeTrans (comp :UIComponent) :void
    {
        comp.setStyle("backgroundAlpha", 0);
        comp.setStyle("backgroundColor", 0);
        comp.setStyle("backgroundImage", null);
    }

    /** A snakeskin filled with our own piss. */
    protected var _gctx :GameContext;

    /** The rematch thing. */
    protected var _rematch :UIComponent;

    /** An area where custom messages may be added after the dialog is up. */
    protected var _topArea :VBox = new VBox();
}
}
