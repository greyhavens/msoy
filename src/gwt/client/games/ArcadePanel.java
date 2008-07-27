//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Page;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Main game display.
 */
public class ArcadePanel extends FlowPanel
{
    public ArcadePanel ()
    {
        setStyleName("arcade");

        add(_header = new GameHeaderPanel((byte)-1, GameInfo.SORT_BY_NAME, null, "Featured Games"));

        CGames.gamesvc.loadArcadeData(CGames.ident, new MsoyCallback<ArcadeData>() {
            public void onSuccess (ArcadeData data) {
                init(data);
            }
        });

    }

    protected void init (ArcadeData data)
    {
        _header.init(data.allGames);

        // show the top N games
        FlowPanel topGames = MsoyUI.createFlowPanel("TopGames");
        topGames.add(MsoyUI.createImage("/images/game/top_games_title.png", "TopGamesTitle"));
        add(topGames);
        for (int i = 0; i < data.topGames.size(); i++) {
            topGames.add(new TopGameWidget(i+1, data.topGames.get(i)));
        }

        add(new FeaturedGamePanel(data.featuredGames));

        add(MsoyUI.createLabel("Browse by Category", "BrowseGenresTitle"));

        // display genre links and browse games in each genre
        FlowPanel browseGenres = MsoyUI.createFlowPanel("BrowseGenres");
        add(browseGenres);
        for (int ii = 0; ii < data.genres.size(); ii++) {
            ArcadeData.Genre genre = data.genres.get(ii);

            // display top games in the genre if there are any
            if (genre.games.length == 0) {
                continue;
            }
            browseGenres.add(new GenreBox(genre));
        }
        browseGenres.add(MsoyUI.createActionLabel(
            "View all games", "ViewAllGames", Link.createListener(Page.GAMES, "g")));
    }

    /**
     * Display a summary of games in a genre.
     */
    protected static class GenreBox extends FlowPanel
    {
        public GenreBox (ArcadeData.Genre genre) {
            setStyleName("GenreBox");

            ClickListener genreClick = Link.createListener(
                Page.GAMES, Args.compose("g", genre.genre));

            FlowPanel header = MsoyUI.createFlowPanel("Header");
            add(header);
            header.add(MsoyUI.createImage("/images/game/genre/" + genre.genre + ".png", "Icon"));
            header.add(MsoyUI.createActionLabel(
                           _dmsgs.getString("genre" + genre.genre), genreClick));

            // display 1-3 games
            for (int i = 0; i < genre.games.length; i++) {
                GameInfo game = genre.games[i];
                ClickListener gameClick = Link.createListener(
                    Page.GAMES, Args.compose("d", game.gameId));
                FlowPanel genreGame = MsoyUI.createFlowPanel("GenreGame");
                add(genreGame);
                // display the first larger than the rest
                if (i == 0) {
                    genreGame.addStyleName("First");
                    genreGame.add(
                        new ThumbBox(game.getThumbMedia(), MediaDesc.THUMBNAIL_SIZE, gameClick));
                    genreGame.add(MsoyUI.createSimplePanel(
                                      "Name", MsoyUI.createActionLabel(game.name, gameClick)));
                    genreGame.add(MsoyUI.createLabel(MsoyUI.truncateParagraph(game.description, 50),
                                                     "Description"));
                }
                else {
                    genreGame.add(new ThumbBox(game.getThumbMedia(),
                                               MediaDesc.HALF_THUMBNAIL_SIZE, gameClick));
                    genreGame.add(MsoyUI.createSimplePanel(
                                      "Name", MsoyUI.createActionLabel(game.name, gameClick)));
                }
            }

            // pad the games area with blank game boxes
            for (int i = genre.games.length; i < ArcadeData.Genre.HIGHLIGHTED_GAMES; i++) {
                add(MsoyUI.createFlowPanel("GenreGame"));
            }

            add(MsoyUI.createSimplePanel("ViewAll", Link.create(
                                             CGames.msgs.genreMore(""+genre.gameCount), Page.GAMES,
                                             Args.compose("g", genre.genre))));

        }
    }

    /**
     * Display a game in the Top X Games list
     */
    protected static class TopGameWidget extends FlowPanel
    {
        public TopGameWidget (int index, GameInfo game) {
            setStyleName("TopGameWidget");
            add(MsoyUI.createLabel(index+"", "Number"));
            ClickListener onClick = Link.createListener(
                Page.GAMES, Args.compose("d", game.gameId));
            add(new ThumbBox(game.getThumbMedia(), MediaDesc.HALF_THUMBNAIL_SIZE, onClick));
            add(MsoyUI.createSimplePanel("Name", MsoyUI.createActionLabel(game.name, onClick)));
        }
    }

    /** Header area with title, games dropdown and search */
    protected GameHeaderPanel _header;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
