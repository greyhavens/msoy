//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameCard;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.PageCallback;

/**
 * Main game display.
 */
public class FBArcadePanel extends FlowPanel
{
    public FBArcadePanel (ArcadeData.Portal portal)
    {
        setStyleName("fbarcade");
        add(MsoyUI.createNowLoading());
        _portal = portal;
        _gamesvc.loadArcadeData(portal, new PageCallback<ArcadeData>(this) {
            public void onSuccess (ArcadeData data) {
                init(data);
            }
        });
    }

    protected void init (final ArcadeData data)
    {
        clear();
        add(_header = new FBGameHeaderPanel(_portal, "Featured Games", GameGenre.ALL,
            GameInfo.Sort.BY_NAME));
        _header.initWithCards(data.allGames);

        // show the top N games
        FlowPanel topGames = MsoyUI.createFlowPanel("TopGames");
        topGames.add(FBFeaturedGamePanel.createTitleBar("title", "Top 10 Games"));
        add(topGames);
        for (int i = 0; i < data.topGames.size(); i++) {
            topGames.add(new TopGameWidget(i + 1, data.topGames.get(i)));
        }

        add(new FBFeaturedGamePanel(data.featuredGames));
    }

    /**
     * Display a game in the Top X Games list
     */
    protected static class TopGameWidget extends SmartTable
    {
        public TopGameWidget (int index, GameCard game) {
            super("TopGameWidget", 0, 0);
            setText(0, 0, index+"", 1, "Number");
            setWidget(0, 1, new ThumbBox(game.thumbMedia, MediaDesc.HALF_THUMBNAIL_SIZE,
                                         Pages.GAMES, "d", game.gameId));
            Widget link = Link.createBlock(game.name, "Name", Pages.GAMES, "d", game.gameId);
            if (game.playersOnline == 0) {
                setWidget(0, 2, link, 1, "Info");
            } else {
                FlowPanel bits = new FlowPanel();
                bits.add(link);
                bits.add(MsoyUI.createLabel(_msgs.featuredOnline(""+game.playersOnline),
                                            "tipLabel"));
                setWidget(0, 2, bits);
            }
        }
    }

    /** Header area with title, games dropdown and search */
    protected FBGameHeaderPanel _header;
    protected ArcadeData.Portal _portal;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
