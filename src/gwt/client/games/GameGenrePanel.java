//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.data.GameGenreData;
import com.threerings.msoy.web.data.GameInfo;

import client.util.MsoyCallback;

/**
 * Displays games in a particular genre.
 */
public class GameGenrePanel extends VerticalPanel
{
    public GameGenrePanel (final byte genre)
    {
        setStyleName("gameGenre");

        CGames.gamesvc.loadGameGenre(CGames.ident, genre, new MsoyCallback() {
            public void onSuccess (Object result) {
                init(genre, (GameGenreData)result);
            }
        });
    }

    protected void init (byte genre, GameGenreData data)
    {
        if (genre >= 0) {
            HorizontalPanel row = new HorizontalPanel();
            row.setStyleName("Features");
            row.add(new WhyPlayPanel());
            row.add(new FeaturedGamePanel(data.featuredGames));
            add(row);
        }

        int rows = (genre >= 0) ? ROWS : FEATURELESS_ROWS;
        PagedGrid games = new PagedGrid(rows, COLUMNS, PagedGrid.NAV_ON_TOP) {
            protected Widget createWidget (Object item) {
                return new GameEntry((GameInfo)item);
            }
            protected String getEmptyMessage () {
                return "No games!";
            }
        };
        games.addStyleName("Games");
        add(games);
        games.setModel(new SimpleDataModel(data.games), 0);
    }

    protected static final int COLUMNS = 3;
    protected static final int ROWS = 3;
    protected static final int FEATURELESS_ROWS = 7;
}
