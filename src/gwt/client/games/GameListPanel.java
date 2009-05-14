//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.game.PlayButton;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.Stars;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.StringUtil;

/**
 * Displays a list of games.
 */
public abstract class GameListPanel extends FlowPanel
{
    public GameListPanel (byte genre, byte sortMethod)
    {
        setStyleName("gameList");
        _genre = genre;

        _sortBox = new ListBox();
        for (int ii = 0; ii < SORT_LABELS.length; ii ++) {
            _sortBox.addItem(SORT_LABELS[ii]);
            if (SORT_VALUES[ii] == sortMethod) {
                _sortBox.setSelectedIndex(ii);
            }
        }
        _sortBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                onSortChanged(SORT_VALUES[_sortBox.getSelectedIndex()]);
            }
        });
    }

    protected abstract void onSortChanged (byte sortMethod);

    /**
     * Displays a grid of games with paging and sort
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
            return _msgs.genreNoGames();
        }

        @Override
        protected boolean displayNavi (int items) {
            return true;
        }

        @Override
        protected void addCustomControls (FlexTable controls) {
            controls.setWidget(0, 0, new InlineLabel(_msgs.genreSortBy(), false, false, false));
            controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
            controls.setWidget(0, 1, _sortBox);

            // add a second row with table titles
            FlowPanel headers = new FlowPanel();
            headers.setStyleName("Titles");
            controls.setWidget(1, 0, headers);
            controls.getFlexCellFormatter().setColSpan(1, 0, 7);

            headers.add(createTitle("Name", "NameTitle", GameInfo.SORT_BY_RATING));
            headers.add(createTitle("Rating", "RatingTitle", GameInfo.SORT_BY_NAME));
            headers.add(createTitle("Category", "CategoryTitle", GameInfo.SORT_BY_GENRE));
            headers.add(createTitle("Now Playing", "NowPlayingTitle",
                                    GameInfo.SORT_BY_PLAYERS_ONLINE));
        }

        @Override
        protected void formatCell (HTMLTable.CellFormatter formatter, int row, int col, int limit) {
            if (row % 2 == 1) {
                formatter.addStyleName(row, col, "Alternating");
            }
        }

        protected Widget createTitle (String text, String styleName, final byte sortMethod) {
            return MsoyUI.createActionLabel(text, styleName, new ClickHandler() {
                public void onClick (ClickEvent event) {
                    onSortChanged(sortMethod);
                }
            });
        }

        protected Widget createPlay (GameInfo game) {
            return PlayButton.create(game, "", PlayButton.Size.SMALL);
        }

        protected class GameInfoPanel extends SmartTable
        {
            public GameInfoPanel (final GameInfo game) {
                setStyleName("GameInfoPanel");
                int col = 0;

                String args = Args.compose("d", game.gameId);
                setWidget(0, col++, MediaUtil.createMediaView(
                              game.thumbMedia, MediaDesc.THUMBNAIL_SIZE,
                              Link.createListener(Pages.GAMES, args)), 1, "Thumbnail");

                FlowPanel name = new FlowPanel();
                name.add(Link.create(game.name, Pages.GAMES, args));
                name.add(MsoyUI.createLabel(MsoyUI.truncateParagraph(game.description, 80),
                         "Description"));
                setWidget(0, col++, name, 1, "NameDesc");

                FlowPanel ratingPanel = new FlowPanel();
                ratingPanel.add(new Stars(game.rating, true, false, null));
                ratingPanel.add(MsoyUI.createLabel(_msgs.genreNumRatings(game.ratingCount+""),
                                "NumRatings"));
                setWidget(0, col++, ratingPanel, 1, "Rating");
                setText(0, col++, _dmsgs.xlate("genre" + game.genre), 1, "Category");
                setText(0, col++, game.playersOnline+"", 1, "NowPlaying");

                setWidget(0, col++, createPlay(game), 1, "PlayButtons");
            }
        }
    }

    /** Dropdown of sort methods */
    protected ListBox _sortBox;

    /** Genre ID or -1 for All Games page */
    protected byte _genre;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final int GAMES_PER_PAGE = 10;

    protected static final String[] SORT_LABELS = new String[] {
        _msgs.genreSortByRating(),
        _msgs.genreSortByNewest(),
        _msgs.genreSortByAlphabetical(),
        _msgs.genreSortByMultiplayer(),
        _msgs.genreSortBySinglePlayer(),
        _msgs.genreSortByCategory(),
        _msgs.genreSortByNowPlaying()
    };

    protected static final byte[] SORT_VALUES = new byte[] {
        GameInfo.SORT_BY_RATING,
        GameInfo.SORT_BY_NEWEST,
        GameInfo.SORT_BY_NAME,
        GameInfo.SORT_BY_GENRE,
        GameInfo.SORT_BY_PLAYERS_ONLINE
    };
}
