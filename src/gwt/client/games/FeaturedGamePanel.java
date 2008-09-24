//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.game.gwt.GameDetail;

import client.shell.Args;
import client.shell.DynamicLookup;
import client.shell.Pages;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.ui.Stars;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays a list of featured games, defaulting to the first one
 */
public class FeaturedGamePanel extends AbsolutePanel
{
    public FeaturedGamePanel (FeaturedGameInfo[] games)
    {
        setStyleName("featuredGame");
        _games = games;
        selectGame(0);
    }

    protected void selectGame (final int index)
    {
        clear();
        final FeaturedGameInfo game = _games[index];

        ClickListener detailsClick =
            Link.createListener(Pages.GAMES, Args.compose("d", game.gameId));
        add(new ThumbBox(game.getShotMedia(), GameDetail.SHOT_WIDTH, GameDetail.SHOT_HEIGHT,
                         detailsClick), 10, 37);
        if (game.playersOnline > 0) {
            add(MsoyUI.createLabel(_msgs.featuredOnline(""+game.playersOnline), "Online"), 10, 170);
        }

        // prev and next buttons are positioned in css
        add(MsoyUI.createPrevNextButtons(new ClickListener() {
            public void onClick (Widget sender) {
                selectGame((index+_games.length-1)%_games.length);
            }
        }, new ClickListener() {
            public void onClick (Widget sender) {
                selectGame((index+1)%_games.length);
            }
        }));

        add(MsoyUI.createLabel(game.name, "Name"), 200, 40);
        add(MsoyUI.createLabel(_dmsgs.xlate("genre" + game.genre), "Genre"), 200, 65);
        add(new CreatorLabel(game.creator), 200, 85);
        add(MsoyUI.createLabel(
                MsoyUI.truncateParagraph(game.description, 100), "Description"), 200, 105);

        add(new Stars(game.rating, true, false, null), 210, 180);
        add(PlayButton.create(game, "", PlayButton.Size.MEDIUM), 307, 160);
    }

    protected FeaturedGameInfo[] _games;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
