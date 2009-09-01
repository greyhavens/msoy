//
// $Id$

package client.games;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.game.PlayButton;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Displays the games created by the caller.
 */
public class MyGamesPanel extends SortedGameListPanel
{
    public MyGamesPanel (final GameInfo.Sort sort)
    {
        super(sort);

        FlowPanel header = MsoyUI.createFlowPanel("gameHeaderPanel");
        FlowPanel absbits = MsoyUI.createFlowPanel("Absolute");
        absbits.add(MsoyUI.createLabel(_msgs.myGames(), "GenreTitle"));
        absbits.add(MsoyUI.createHTML(_msgs.myGamesCreateTip(), "CreateTip"));
        absbits.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.myGamesCreate(),
                                        Link.createHandler(Pages.GAMES, "c")));
        header.add(absbits);
        header.add(MsoyUI.createLabel(_msgs.myGamesDevTip(), "DevTip"));
        add(header);

        _gamesvc.loadMyGames(new InfoCallback<List<GameInfo>>() {
            public void onSuccess (List<GameInfo> games) {
                Collections.sort(games, sort.comparator);
                add(new GameGrid(games));
            }
        });
    }

    @Override // from GameListPanel
    protected Widget createActionWidget (GameInfo game)
    {
        FlowPanel bits = new FlowPanel();
        // we want our play buttons to play the development version
        bits.add(PlayButton.createSmall(GameInfo.toDevId(game.gameId)));
        bits.add(WidgetUtil.makeShim(5, 5));
        bits.add(Link.create(_msgs.myGamesEdit(), Pages.GAMES, "e", game.gameId));
        return bits;
    }

    protected void onSortChanged (GameInfo.Sort sort)
    {
        Link.go(Pages.GAMES, "m", sort.toToken());
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
