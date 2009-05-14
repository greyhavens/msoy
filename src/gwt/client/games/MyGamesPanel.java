//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.game.data.all.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays the games created by the caller.
 */
public class MyGamesPanel extends GameListPanel
{
    public MyGamesPanel (byte sortMethod)
    {
        super(GameGenre.ALL, sortMethod);

        _gamesvc.loadMyGames(new InfoCallback<List<GameInfo>>() {
            public void onSuccess (List<GameInfo> games) {
                init(games);
            }
        });
    }

    protected void onSortChanged (byte sortMethod)
    {
        Link.go(Pages.GAMES, Args.compose("mine", sortMethod));
    }

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
