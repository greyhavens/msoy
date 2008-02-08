//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.ArcadeData;
import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GameInfo;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Main game display.
 */
public class ArcadePanel extends VerticalPanel
{
    public ArcadePanel ()
    {
        setStyleName("arcade");
        setSpacing(5);

        CGame.gamesvc.loadArcadeData(CGame.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((ArcadeData)result);
            }
        });
    }

    protected void init (ArcadeData data)
    {
        HorizontalPanel row = new HorizontalPanel();
        row.add(new MyGamesPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new FeaturedGamePanel(data.featuredGame));
        add(row);

        for (int ii = 0; ii < data.genres.size(); ii++) {
            if (ii % 3 == 0) {
                row = new HorizontalPanel();
                add(row);
            }
            row.add(new GameGenrePanel((ArcadeData.Genre)data.genres.get(ii)));
            if (ii % 3 != 2) {
                row.add(WidgetUtil.makeShim(5, 5));
            }
        }
    }

    protected static class MyGamesPanel extends FlexTable
    {
        public MyGamesPanel ()
        {
            setStyleName("MyGames");
        }
    }

    protected static class GameGenrePanel extends FlexTable
    {
        public GameGenrePanel (ArcadeData.Genre genre) {
            setStyleName("Genre");
            setCellPadding(0);
            setCellSpacing(0);

            int row = 0;
            setText(row, 0, CGame.dmsgs.getString("genre" + genre.genre));
            getFlexCellFormatter().setStyleName(row++, 0, "Title");

            for (int ii = 0; ii < genre.games.length; ii++) {
                setWidget(row++, 0, new GameInfoPanel(genre.games[ii]));
            }

            setWidget(row, 0, Application.createLink(CGame.msgs.genreMore(""+genre.gameCount),
                                                     Page.GAME, Args.compose("g", genre.genre)));
            getFlexCellFormatter().setStyleName(row++, 0, "More");
        }
    }

    protected static class GameInfoPanel extends FlexTable
    {
        public GameInfoPanel (final GameInfo game) {
            setStyleName("Game");

            ClickListener onClick = new ClickListener() {
                public void onClick (Widget widget) {
                    Application.go(Page.GAME, Args.compose("d", game.gameId));
                }
            };
            setWidget(0, 0, MediaUtil.createMediaView(
                          game.getThumbMedia(), MediaDesc.THUMBNAIL_SIZE, onClick));
            getFlexCellFormatter().setStyleName(0, 0, "Thumb");

            setWidget(0, 1, MsoyUI.createActionLabel(game.name, onClick));
            getFlexCellFormatter().setStyleName(0, 1, "Name");

            setText(1, 0, truncate(game.description));
            getFlexCellFormatter().setStyleName(1, 0, "Descrip");

            setText(2, 0, (game.playersOnline == 1) ? CGame.msgs.genrePlayer() :
                    CGame.msgs.genrePlayers(""+game.playersOnline));
            getFlexCellFormatter().setStyleName(2, 0, "Players");
            getFlexCellFormatter().setRowSpan(0, 0, 3);
        }

        protected static String truncate (String descrip)
        {
            if (descrip.length() <= MAX_DESCRIP_LENGTH) {
                return descrip;
            }
            for (int ii = 0; ii < MAX_DESCRIP_LENGTH; ii++) {
                char c = descrip.charAt(ii);
                if (c == '.' || c == '!') {
                    return descrip.substring(0, ii+1);
                }
            }
            return descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
        }

        protected static final int MAX_DESCRIP_LENGTH = 50;
    }
}
