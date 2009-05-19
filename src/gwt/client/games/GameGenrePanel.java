//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays games in a particular genre, or of all genres for the "All Games" page.
 * For genre pages it displays a featured game; for "All Games" page displays a search.
 */
public class GameGenrePanel extends GameListPanel
{
    public GameGenrePanel (byte genre, GameInfo.Sort sort, String query)
    {
        super(genre, sort);

        String titleText;
        if (genre >= 0) {
            String genreTitle = _dmsgs.xlate("genre" + genre);
            titleText = (genreTitle.indexOf("/") != -1) ? genreTitle : _msgs.genreWrap(genreTitle);
        } else {
            titleText = _msgs.genreAllGames();
        }
        add(_header = new GameHeaderPanel(titleText, genre, sort));
        _header.setQuery(query);

        _gamesvc.loadGameGenre(genre, sort, query, new InfoCallback<List<GameInfo>>() {
            public void onSuccess (List<GameInfo> games) {
                _header.initWithInfos(games); // set the dropdown list of all games
                add(new GameGrid(games));
            }
        });
    }

    protected void onSortChanged (GameInfo.Sort sort)
    {
        Link.go(Pages.GAMES, "g", _genre, sort.toToken(), _header.getQuery());
    }

    /** Header area with title, games dropdown and search */
    protected GameHeaderPanel _header;

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
