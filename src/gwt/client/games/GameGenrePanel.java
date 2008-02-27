//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GameGenreData;
import com.threerings.msoy.web.data.GameInfo;

import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Displays games in a particular genre.
 */
public class GameGenrePanel extends VerticalPanel
{
    public GameGenrePanel (byte genre)
    {
        setStyleName("gameGenre");

        CGames.gamesvc.loadGameGenre(CGames.ident, genre, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((GameGenreData)result);
            }
        });
    }

    protected void init (GameGenreData data)
    {
        HorizontalPanel row = new HorizontalPanel();
        row.add(new WhyPlayPanel());
        row.add(new FeaturedGamePanel(data.featuredGames));
        add(row);

        PagedGrid games = new PagedGrid(5, 3, PagedGrid.NAV_ON_TOP) {
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
}
