//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
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

        CGames.gamesvc.loadArcadeData(CGames.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((ArcadeData)result);
            }
        });
    }

    protected void init (ArcadeData data)
    {
        HorizontalPanel row = new HorizontalPanel();
        row.add(new WhyPlayPanel());
        row.add(WidgetUtil.makeShim(5, 5));
        row.add(new FeaturedGamePanel(data.featuredGames)); // TODO: next/prev
        add(row);

        for (int ii = 0; ii < data.genres.size(); ii++) {
            if (ii % 3 == 0) {
                row = new HorizontalPanel();
                add(row);
            }
            ArcadeData.Genre genre = (ArcadeData.Genre)data.genres.get(ii);
            row.add(MsoyUI.createBox(null, CGames.dmsgs.getString("genre" + genre.genre),
                                     new GenreSummaryPanel(genre)));
            if (ii % 3 != 2) {
                row.add(WidgetUtil.makeShim(5, 5));
            }
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

            setWidget(row, 0, Application.createLink(CGames.msgs.genreMore(""+genre.gameCount),
                                                     Page.GAMES, Args.compose("g", genre.genre)));
            getFlexCellFormatter().setStyleName(row++, 0, "More");
        }
    }
}
