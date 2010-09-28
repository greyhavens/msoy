//
// $Id$

package client.edgames;

import com.google.gwt.core.client.GWT;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Page;

/**
 * Displays a page that allows a player to play a particular game. If it's single player the game
 * is shown, if it's multiplayer the lobby is first shown where the player can find opponents
 * against which to play.
 */
public class EditGamesPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we have d-NNN then we want to see game detail
        String action = args.get(0, "");
        if (action.equals("m") || action.equals("")) {
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

        } else if (action.equals("emg")) {
            setContent(new EditMochiGamesPanel());

        } else if (action.equals("eft")) {
            setContent(new EditGlobalFeedThumbsPanel());

        } else if (action.equals("aa")) {
            byte portal = args.get(1, ArcadeData.Portal.MAIN.toByte());
            setContent(new AddArcadeEntriesPanel(
                ByteEnumUtil.fromByte(ArcadeData.Portal.class, portal),
                GameInfo.Sort.fromToken(args.get(1, "")), args.get(3, (String)null)));
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.EDGAMES;
    }

    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
}
