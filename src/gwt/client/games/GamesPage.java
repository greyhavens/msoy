//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.samskivert.depot.util.ByteEnumUtil;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil.GameDetails;

import client.notifications.NotificationsPanel;
import client.shell.CShell;
import client.shell.Page;

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
            panel.setGame(args.get(1, 0), GameDetails.getByCode(args.get(2, "")));

        } else if (action.equals("t")) {
            setContent(new TrophyCasePanel(args.get(1, 0)));

        } else if (action.equals("ct")) {
            setContent(_msgs.compareTitle(),
                new TrophyComparePanel(args.get(1, 0), args.get(2, 0)));

        } else if (action.equals("g")) {
            setContent(new GameGenrePanel(getDefaultPortal(),
                ByteEnumUtil.fromByte(GameGenre.class, args.get(1, GameGenre.ALL.toByte())),
                GameInfo.Sort.fromToken(args.get(2, "")), args.get(3, null)));

        } else if (action.equals("m")) {
            setContent(new MyGamesPanel(GameInfo.Sort.fromToken(args.get(1, ""))));

        } else if (action.equals("c")) {
            setContent(_msgs.cgTitle(), new CreateGamePanel());

        } else if (action.equals("e")) {
            EditGamePanel panel;
            if (getContent() instanceof EditGamePanel) {
                panel = (EditGamePanel)getContent();
            } else {
                setContent(_msgs.egTitle(), panel = new EditGamePanel());
            }
            panel.setGame(args.get(1, 0), args.get(2, 0));

        } else if (action.equals("ea")) {
            setContent(new EditArcadePanel());

        } else if (action.equals("aa")) {
            byte portal = args.get(1, ArcadeData.Portal.MAIN.toByte());
            setContent(new AddArcadeEntriesPanel(
                ByteEnumUtil.fromByte(ArcadeData.Portal.class, portal),
                GameInfo.Sort.fromToken(args.get(1, "")), args.get(3, null)));

        } else if (action.equals("mochi")) {
            String mochiTag = args.get(1, "");
            setContent(new MochiGamePanel(mochiTag));

        } else {
            if (CShell.isFacebook()
                    || action.equals("fb")) { // TEMP: "fb" action added by Ray for testing
                // show the facebook portal and request to show the friends panel
                FlowPanel arcade = new FlowPanel();
                arcade.add(new NotificationsPanel());
                arcade.add(new FBArcadePanel());
                setContent(arcade);
                CShell.frame.openBottomFrame(Pages.FACEBOOK.makeToken("friends"));

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
        return CShell.isFacebook() ? ArcadeData.Portal.FACEBOOK : ArcadeData.Portal.MAIN;
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
