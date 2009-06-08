//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.game.PlayButton;
import client.shell.DynamicLookup;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.ui.Stars;
import client.ui.ThumbBox;

/**
 * Displays a list of featured games, defaulting to the first one
 */
public class FeaturedGamePanel extends AbsolutePanel
{
    public FeaturedGamePanel (GameInfo[] games)
    {
        setStyleName("featuredGame");
        _games = games;
        if (games.length > 0) {
            selectGame(0);
        }
    }

    protected void selectGame (final int index)
    {
        clear();
        final GameInfo game = _games[index];

        add(new ThumbBox(game.shotMedia, MediaDesc.GAME_SHOT_SIZE, Pages.GAMES, "d", game.gameId),
            10, 37);
        if (game.playersOnline > 0) {
            add(MsoyUI.createLabel(_msgs.featuredOnline(""+game.playersOnline), "Online"), 10, 170);
        }

        // prev and next buttons are positioned in css
        add(MsoyUI.createPrevNextButtons(new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectGame((index+_games.length-1)%_games.length);
            }
        }, new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectGame((index+1)%_games.length);
            }
        }));

        add(MsoyUI.createLabel(game.name, "Name"), 200, 40);
        add(MsoyUI.createLabel(_dmsgs.xlate("genre_" + game.genre), "Genre"), 200, 65);
        add(new CreatorLabel(game.creator), 200, 85);
        add(MsoyUI.createLabel(
                MsoyUI.truncateParagraph(game.description, 100), "Description"), 200, 105);

        add(new Stars(game.rating, true, false, null), 210, 180);
        add(PlayButton.createMedium(game.gameId), 307, 160);
    }

    protected GameInfo[] _games;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
