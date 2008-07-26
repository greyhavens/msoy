//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.web.data.FeaturedGameInfo;

import client.shell.Args;
import client.shell.Page;
import client.util.CreatorLabel;
import client.util.Link;
import client.util.MsoyUI;
import client.util.Stars;
import client.util.ThumbBox;

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

        ClickListener detailsClick = Link.createListener(
            Page.GAMES, Args.compose("d", game.gameId));
        add(new ThumbBox(game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT,
                         detailsClick), 10, 37);
        if (game.playersOnline > 0) {
            add(MsoyUI.createLabel(
                    CGames.msgs.featuredOnline("" + game.playersOnline), "Online"), 10, 170);
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
        add(MsoyUI.createLabel(CGames.dmsgs.getString("genre" + game.genre), "Genre"), 200, 65);
        add(new CreatorLabel(game.creator), 200, 85);
        add(MsoyUI.createLabel(
                MsoyUI.truncateParagraph(game.description, 100), "Description"), 200, 105);

        add(new Stars(game.rating, true, false, null), 210, 180);
        PlayButton play = new PlayButton(game.gameId, game.minPlayers, game.maxPlayers);
        play.setStyleName("playButtonSmall");
        add(play, 307, 160);
    }

    protected FeaturedGameInfo[] _games;
}
