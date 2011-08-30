//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.Stars;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays a list of games.
 */
public abstract class GameListPanel extends FlowPanel
{
    /**
     * Creates a new list.
     */
    public GameListPanel ()
    {
        setStyleName("gameList");
    }

    /**
     * Creates a new widget for taking some action on a game in the game grid.
     */
    protected Widget createActionWidget (GameInfo game)
    {
        return PlayButton.createSmall(game.gameId);
    }

    /**
     * Adds custom controls for the game grid at the given row. Returns the next available row
     * for adding more custom controls.
     */
    protected int addCustomControls (FlexTable controls, int row) {
        // add a row with table titles
        FlowPanel headers = new FlowPanel();
        headers.setStyleName("Titles");
        controls.setWidget(row, 0, headers);
        controls.getFlexCellFormatter().setColSpan(row, 0, 7);

        headers.add(createTitle("Name", "NameTitle", GridColumn.NAME));
        headers.add(createTitle("Rating", "RatingTitle", GridColumn.RATING));
        headers.add(createTitle("Category", "CategoryTitle", GridColumn.CATEGORY));
        headers.add(createTitle("Now Playing", "NowPlayingTitle", GridColumn.NOW_PLAYING));

        return ++row;
    }

    /**
     * Creates a widget representing a column of the game grid.
     */
    protected Widget createTitle (String text, String styleName, GridColumn column) {
        return MsoyUI.createLabel(text, styleName);
    }

    /**
     * Returns a message to display when the list is empty.
     */
    protected String getEmptyMessage ()
    {
        return _gmsgs.genreNoGames();
    }

    /**
     * The columns that appear in the game grid.
     */
    protected enum GridColumn { NAME, RATING, CATEGORY, NOW_PLAYING }

    /**
     * Displays a grid of games with paging.
     */
    protected class GameGrid extends PagedGrid<GameInfo>
    {
        public GameGrid (List<GameInfo> games) {
            super(GAMES_PER_PAGE, 1, PagedGrid.NAV_ON_TOP);
            addStyleName("Games");
            setModel(new SimpleDataModel<GameInfo>(games), 0);
        }

        @Override
        protected Widget createWidget (GameInfo item) {
            return new GameInfoPanel(item);
        }

        @Override
        protected String getEmptyMessage () {
            return GameListPanel.this.getEmptyMessage();
        }

        @Override
        protected boolean displayNavi (int items) {
            return true;
        }

        @Override
        protected void addCustomControls (FlexTable controls) {
            GameListPanel.this.addCustomControls(controls, 0);
        }

        @Override
        protected void formatCell (HTMLTable.CellFormatter formatter, int row, int col, int limit) {
            if (row % 2 == 1) {
                formatter.addStyleName(row, col, "Alternating");
            }
        }

        protected class GameInfoPanel extends SmartTable
        {
            public GameInfoPanel (final GameInfo game) {
                setStyleName("GameInfoPanel");
                int col = 0;

                setWidget(0, col++, MediaUtil.createMediaView(
                              game.thumbMedia, MediaDescSize.THUMBNAIL_SIZE,
                              Link.createHandler(Pages.GAMES, "d", game.gameId)), 1, "Thumbnail");

                FlowPanel name = new FlowPanel();
                name.add(Link.create(game.name, Pages.GAMES, "d", game.gameId));
                name.add(MsoyUI.createLabel(MsoyUI.truncateParagraph(game.description, 80),
                         "Description"));
                setWidget(0, col++, name, 1, "NameDesc");

                FlowPanel ratingPanel = new FlowPanel();
                ratingPanel.add(new Stars(game.rating, true, false, null));
                ratingPanel.add(MsoyUI.createLabel(_gmsgs.genreNumRatings(game.ratingCount+""),
                                "NumRatings"));
                setWidget(0, col++, ratingPanel, 1, "Rating");
                setText(0, col++, _dmsgs.xlate("genre_" + game.genre), 1, "Category");
                setText(0, col++, game.playersOnline+"", 1, "NowPlaying");

                setWidget(0, col, createActionWidget(game), 1, "PlayButtons");
                getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_CENTER);
            }
        }
    }

    protected static final GameMessages _gmsgs = GWT.create(GameMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final int GAMES_PER_PAGE = 10;
}
