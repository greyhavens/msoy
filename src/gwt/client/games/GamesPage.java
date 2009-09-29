//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil.GameDetails;

import client.game.GameGenrePanel;
import client.reminders.RemindersPanel;
import client.shell.CShell;
import client.shell.Page;
import client.util.Link;

/**
 * Displays a page that allows a player to play a particular game. If it's single player the game
 * is shown, if it's multiplayer the lobby is first shown where the player can find opponents
 * against which to play.
 */
public class GamesPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we have d-NNN then we want to see game detail
        String action = args.get(0, "");
        if (action.equals("d")) {
            GameDetailPanel panel;
            if (getContent() instanceof GameDetailPanel) {
                panel = (GameDetailPanel)getContent();
            } else {
                setContent(panel = new GameDetailPanel());
            }
            int gameId = args.get(1, 0);
            panel.setGame(gameId, GameDetails.getByCode(args.get(2, "")));
            showFriendsBar(gameId);

        } else if (action.equals("t")) {
            setContent(new TrophyCasePanel(args.get(1, 0)));
            showFriendsBar(0);

        } else if (action.equals("ct")) {
            setContent(_msgs.compareTitle(),
                new TrophyComparePanel(args.get(1, 0), args.get(2, 0)));

        } else if (action.equals("g")) {
            setContent(new GameGenrePanel(getDefaultPortal(),
                ByteEnumUtil.fromByte(GameGenre.class, args.get(1, GameGenre.ALL.toByte())),
                GameInfo.Sort.fromToken(args.get(2, "")), args.get(3, null)));
            showFriendsBar(0);

        } else if (action.equals("m") || action.equals("c") || action.equals("e") ||
            action.equals("ea") || action.equals("emg") || action.equals("eft") ||
            action.equals("aa")) {
            // redirect to edgames and log (in case anyone is watching the log and can report it)
            CShell.log("Broken games link", "action", action);
            Link.go(Pages.EDGAMES, args);

        } else if (action.equals("mochi")) {
            String mochiTag = args.get(1, "");
            setContent(new MochiGamePanel(mochiTag));
            showFriendsBar(0); // just show the generic friends bar for mochi games

        } else {
            if (CShell.getClientMode().isFacebookGames()
                    || action.equals("fb")) { // TEMP: "fb" action added by Ray for testing
                // show the facebook portal and request to show the friends panel
                FlowPanel arcade = new FlowPanel();
                arcade.add(new RemindersPanel());
                arcade.add(new FBArcadePanel());
                setContent(arcade);
                showFriendsBar(0);

            } else {
                setContent(new ArcadePanel(getDefaultPortal()));
            }
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.GAMES;
    }

    protected ArcadeData.Portal getDefaultPortal ()
    {
        return CShell.getClientMode().isFacebookGames() ?
            ArcadeData.Portal.FACEBOOK : ArcadeData.Portal.MAIN;
    }

    protected void showFriendsBar (int gameId)
    {
        if (!CShell.getClientMode().isFacebookGames()) {
            return;
        }

        if (gameId == 0) {
            CShell.log("Opening bottom friends frame");
            CShell.frame.openBottomFrame(Pages.FACEBOOK.makeToken("friends"));
        } else {
            CShell.log("Opening bottom friends frame in game mode", "gameId", gameId);
            CShell.frame.openBottomFrame(Pages.FACEBOOK.makeToken("game", gameId));
        }
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
