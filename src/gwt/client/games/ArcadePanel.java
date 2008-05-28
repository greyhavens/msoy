//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.data.ArcadeData;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.HeaderBox;
import client.util.MsoyCallback;

/**
 * Main game display.
 */
public class ArcadePanel extends VerticalPanel
{
    public ArcadePanel ()
    {
        setStyleName("arcade");

        CGames.gamesvc.loadArcadeData(CGames.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((ArcadeData)result);
            }
        });
    }

    protected void init (ArcadeData data)
    {
        HorizontalPanel top = new HorizontalPanel();
        top.setStyleName("Features");
        top.add(new WhyPlayPanel());
        top.add(new FeaturedGamePanel(data.featuredGames));
        add(top);

        Grid grid = new Grid(3, 3);
        grid.setCellSpacing(5);
        grid.setCellPadding(0);
        add(grid);

        for (int ii = 0; ii < data.genres.size(); ii++) {
            ArcadeData.Genre genre = (ArcadeData.Genre)data.genres.get(ii);
            int row = ii/3, col = ii%3;
            grid.setWidget(row, col, new HeaderBox("/images/game/genre/" + genre.genre + ".png",
                                                   CGames.dmsgs.getString("genre" + genre.genre),
                                                   new GenreSummaryPanel(genre)));
            grid.getCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_TOP);
        }
    }

    protected static class MyGamesPanel extends SmartTable
    {
        public MyGamesPanel ()
        {
            setText(0, 0, "Coming soon!");
        }
    }

    protected static class GenreSummaryPanel extends SmartTable
    {
        public GenreSummaryPanel (ArcadeData.Genre genre) {
            super("Genre", 0, 0);

            int row = 0;
            for (int ii = 0; ii < genre.games.length; ii++) {
                setWidget(row++, 0, new GameEntry(genre.games[ii]));
            }
            for (int ii = genre.games.length; ii < 2; ii++) {
                SmartTable shim = new SmartTable("gameEntry", 0, 0);
                shim.setHTML(0, 0, "&nbsp;");
                setWidget(row++, 0, shim);
            }

            setWidget(row, 0, Application.createLink(CGames.msgs.genreMore(""+genre.gameCount),
                                                     Page.GAMES, Args.compose("g", genre.genre)));
            getFlexCellFormatter().setStyleName(row++, 0, "More");
        }
    }
}
