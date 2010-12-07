//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameCard;
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
public class FBArcadePanel extends AbsoluteCSSPanel
{
    public FBArcadePanel ()
    {
        super("fbarcade", "fixed");
        add(MsoyUI.createNowLoading());
        _gamesvc.loadArcadeData(ArcadeData.Portal.FACEBOOK, new PageCallback<ArcadeData>(this) {
            public void onSuccess (ArcadeData data) {
                init(data);
            }
        });
    }

    protected void init (final ArcadeData data)
    {
        clear();
        add(_header = new FBGameHeaderPanel());
        _header.initWithCards(data.allGames);

        // top games
        FlowPanel topGames = MsoyUI.createFlowPanel("TopGames");
        topGames.add(MsoyUI.createLabel("Top 10 Games", "title"));
        add(topGames);
        DOM.setStyleAttribute(topGames.getElement(), "position", "absolute");
        int ii;
        for (ii = 0; ii < data.topGames.size(); ii++) {
            topGames.add(createTopGameWidget(ii + 1, data.topGames.get(ii)));
        }

        // pad out for testing
        if (data.topGames.size() > 0) {
            for (; ii < 10; ++ii) {
                int rnd = (int)(Math.random() * data.topGames.size());
                topGames.add(createTopGameWidget(ii + 1, data.topGames.get(rnd)));
            }
        }

        // featured games
        //add(new FBFeaturedGamePanel(data.featuredGames));
        add(new FBMochiGamePanel(data.mochiGames));

        // game wall
        AbsolutePanel gameWall = new AbsolutePanel();
        gameWall.setStyleName("GameWall");
        gameWall.add(MsoyUI.createLabel("Featured Games", "title"));
        SmartTable grid = new SmartTable("Grid", 0, 0);
        gameWall.add(grid);
        int cell = 1;
        for (GameCard wallGame : data.gameWall) {
            grid.setWidget(cell / 3, cell % 3, createWallGameWidget(wallGame), 1, "WallCell");
            cell++;
        }

        // pad out for testing
        if (data.topGames.size() > 0) {
            for (;cell <= 20; cell++) {
                int rnd = (int)(Math.random() * data.topGames.size());
                grid.setWidget(cell / 3, cell % 3,
                    createWallGameWidget(data.topGames.get(rnd)), 1, "WallCell");
            }
        }

        add(gameWall);
    }

    /**
     * Creates a widget displaying a top game.
     */
    protected SmartTable createTopGameWidget (int index, GameCard game)
    {
        SmartTable table = new SmartTable("TopGameWidget", 0, 0);
        table.setText(0, 0, index+"", 1, "Number");
        addGame(table, 0, 1, game);
        return table;
    }

    /**
     * Creates a widget displaying a wall game.
     */
    protected SmartTable createWallGameWidget (GameCard game)
    {
        SmartTable table = new SmartTable("WallGameWidget", 0, 0);
        addGame(table, 0, 0, game);
        return table;
    }

    protected void addGame (SmartTable table, int row, int startCol, GameCard game)
    {
        table.setWidget(row, startCol, new ThumbBox(game.thumbMedia, MediaDescSize.HALF_THUMBNAIL_SIZE,
                                                    Pages.GAMES, "d", game.gameId));
        Widget link = Link.createBlock(game.name, "Name", Pages.GAMES, "d", game.gameId);
        if (game.playersOnline == 0) {
            table.setWidget(row, startCol+1, link, 1, "Info");
        } else {
            FlowPanel bits = new FlowPanel();
            bits.add(link);
            bits.add(MsoyUI.createLabel(_msgs.featuredOnline(""+game.playersOnline), "tipLabel"));
            table.setWidget(row, startCol+1, bits);
        }
    }

    /** Header area with title, games dropdown and search */
    protected FBGameHeaderPanel _header;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
