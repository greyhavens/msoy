//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Page;
import client.ui.MsoyUI;
import client.ui.Stars;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays games in a particular genre, or of all genres for the "All Games" page.
 * For genre pages it displays a featured game; for "All Games" page displays a search.
 */
public class GameGenrePanel extends FlowPanel
{
    public GameGenrePanel (final byte genre, final byte sortMethod, final String query)
    {
        setStyleName("gameGenre");
        _genre = genre;

        _sortBox = new ListBox();
        for (int ii = 0; ii < SORT_LABELS.length; ii ++) {
            _sortBox.addItem(SORT_LABELS[ii]);
            if (SORT_VALUES[ii] == sortMethod) {
                _sortBox.setSelectedIndex(ii);
            }
        }
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                byte newSortMethod = SORT_VALUES[((ListBox)widget).getSelectedIndex()];
                if (query == null) {
                    Link.go(Page.GAMES, Args.compose(
                        new String[] {"g", genre+"", newSortMethod+""}));
                }
                else {
                    Link.go(Page.GAMES, Args.compose(
                        new String[] {"g", genre+"", newSortMethod+"", query}));
                }

            }
        });

        String titleText;
        if (genre >= 0) {
            String genreTitle = _dmsgs.getString("genre" + genre);
            if (genreTitle.length() > 8) {
                titleText = genreTitle;
            }
            else {
                titleText = genreTitle + " Games";
            }
        }
        else {
            titleText = _msgs.genreAllGames();
        }
        add(_header = new GameHeaderPanel(genre, sortMethod, query, titleText));

        _gamesvc.loadGameGenre(
            CGames.ident, genre, sortMethod, query, new MsoyCallback<List<GameInfo>>() {
                public void onSuccess (List<GameInfo> games) {
                    init(genre, games);
                }
        });
    }

    /**
     * After data is received, display the genre header and the data grid
     */
    protected void init (byte genre, List<GameInfo> games)
    {
        // set the dropdown list of all games
        _header.init(games);

        // add the games to the page
        add(new GameGenreGrid(games));
    }

    /**
     * Displays a grid of games with paging and sort
     */
    protected class GameGenreGrid extends PagedGrid<GameInfo>
    {
        public GameGenreGrid (List<GameInfo> games) {
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

        /**
         * Add the sort box and header row
         */
        @Override
        protected void addCustomControls (FlexTable controls) {
            controls.setWidget(
                0, 0, new InlineLabel(_msgs.genreSortBy(), false, false, false));
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

        /**
         * Creates a title for display in the grid header that performs a sort action onclick
         */
        protected Widget createTitle (String text, String styleName, byte sortMethod) {
            Widget link = Link.create(
                text, Page.GAMES, Args.compose(new String[] {"g", _genre+"", sortMethod+""}));
            link.addStyleName(styleName);
            return link;
        }

        /**
         * Alternate row styles
         */
        @Override
        protected void formatCell (HTMLTable.CellFormatter formatter, int row, int col, int limit)
        {
            if (row % 2 == 1) {
                formatter.addStyleName(row, col, "Alternating");
            }
        }

        /**
         * One row of the grid with details on a single game.
         */
        protected class GameInfoPanel extends SmartTable
        {
            public GameInfoPanel (final GameInfo game)
            {
                setStyleName("GameInfoPanel");
                int col = 0;

                ClickListener gameClick = new ClickListener() {
                    public void onClick (Widget widget) {
                        Link.go(Page.GAMES, Args.compose("d", game.gameId));
                    }
                };
                setWidget(0, col++, MediaUtil.createMediaView(
                    game.thumbMedia, MediaDesc.THUMBNAIL_SIZE, gameClick), 1, "Thumbnail");

                FlowPanel name = new FlowPanel();
                name.add(MsoyUI.createActionLabel(game.name, "Name", gameClick));
                name.add(MsoyUI.createLabel(MsoyUI.truncateParagraph(game.description, 80),
                         "Description"));
                setWidget(0, col++, name, 1, "NameDesc");

                FlowPanel ratingPanel = new FlowPanel();
                ratingPanel.add(new Stars(game.rating, true, false, null));
                ratingPanel.add(MsoyUI.createLabel(_msgs.genreNumRatings(game.ratingCount+""),
                                "NumRatings"));
                setWidget(0, col++, ratingPanel, 1, "Rating");
                setText(0, col++, _dmsgs.getString("genre" + game.genre), 1, "Category");
                setText(0, col++, game.playersOnline+"", 1, "NowPlaying");

                FlowPanel playButtonsPanel = new FlowPanel();
                playButtonsPanel.setStyleName("PlayButtonsPanel");
                // add single player button
                if (game.minPlayers == 1) {
                    ClickListener singleClick = new ClickListener() {
                        public void onClick (Widget sender) {
                            Link.go(Page.WORLD, Args.compose("game", "s", "" + game.gameId));
                        }
                    };
                    PushButton single = MsoyUI.createButton(
                        MsoyUI.MEDIUM_THIN, "Play Just Me", singleClick);
                    single.addStyleName("PlaySingleButton");
                    playButtonsPanel.add(single);
                    if (game.maxPlayers > 1) {
                        playButtonsPanel.add(WidgetUtil.makeShim(5, 5));
                    }
                }
                // add multiplayer button
                if (game.maxPlayers > 1) {
                    ClickListener multiClick = new ClickListener() {
                        public void onClick (Widget sender) {
                            Link.go(Page.WORLD, Args.compose("game", "l", "" + game.gameId));
                        }
                    };
                    PushButton multi = MsoyUI.createButton(
                        MsoyUI.MEDIUM_THIN, "Play With Friends", multiClick);
                    multi.addStyleName("PlayMultiButton");
                    playButtonsPanel.add(multi);
                }
                setWidget(0, col++, playButtonsPanel, 1, "PlayButtons");
            }
        }
    }

    /** Header area with title, games dropdown and search */
    protected GameHeaderPanel _header;

    /** Dropdown of sort methods */
    protected ListBox _sortBox;

    /** Genre ID or -1 for All Games page */
    protected byte _genre;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);

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
        GameInfo.SORT_BY_MULTIPLAYER,
        GameInfo.SORT_BY_SINGLE_PLAYER,
        GameInfo.SORT_BY_GENRE,
        GameInfo.SORT_BY_PLAYERS_ONLINE
    };
}
