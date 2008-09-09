//
// $Id$

package client.games;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.Game;

import client.comment.CommentsPanel;
import client.item.ItemRating;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.StyledTabPanel;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NaviUtil;
import client.util.ServiceUtil;
import client.util.NaviUtil.GameDetails;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends SmartTable
    implements TabListener
{
    public GameDetailPanel ()
    {
        super("gameDetail", 0, 10);
    }

    public void setGame (final int gameId, final GameDetails tab)
    {
        if (_gameId == gameId) {
            selectTab(tab);
        } else {
            _gamesvc.loadGameDetail(gameId, new MsoyCallback<GameDetail>() {
                public void onSuccess (GameDetail detail) {
                    setGameDetail(gameId, detail);
                    selectTab(tab);
                }
            });
        }
    }

    public void setGameDetail (int gameId, GameDetail detail)
    {
        // if we are referencing the listed item but it does not exist, switch to the development
        // (source) item which always exists, and which has a negative id.
        if (!Game.isDeveloperVersion(gameId) && detail.listedItem == null) {
            gameId = gameId * -1;
        }

        // Note: the gameId may be the negative original gameId, but GameDetail's id is never
        // negative to match
        _gameId = gameId;
        CShell.frame.setTitle(detail.getGame().name);

        Game game = detail.getGame();
        VerticalPanel shot = new VerticalPanel();
        shot.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        shot.add(new ThumbBox(game.getShotMedia(), GameDetail.SHOT_WIDTH,
                              GameDetail.SHOT_HEIGHT, null));
        if (detail.listedItem != null) {
            shot.add(WidgetUtil.makeShim(5, 5));
            shot.add(new ItemRating(detail.listedItem, detail.memberItemInfo, false));
        }
        setWidget(0, 0, shot);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        setWidget(0, 1, new GameNamePanel(
                      game.name, game.genre, detail.creator, game.description), 2, null);

        setWidget(1, 0, new GameBitsPanel(
            detail.minPlayers, detail.maxPlayers, detail.averageDuration, detail.gamesPlayed,
            detail.sourceItem.itemId));

        FlowPanel play = new FlowPanel();
        play.setStyleName("playPanel");
        Widget playbut = new PlayButton(gameId, detail.minPlayers, detail.maxPlayers);
        play.add(playbut);

        // if (detail.playingNow > 0) {
            play.add(MsoyUI.createLabel(_msgs.featuredOnline(""+detail.playingNow), "Online"));
        // }
        setWidget(1, 1, play, 1, "Play");
        getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_CENTER);

        // add "Discussions" and "Shop" button if appropriate
        Widget buttons = null;
        if (game.groupId > 0) {
            ClickListener onClick = Link.createListener(
                Pages.WHIRLEDS, Args.compose("f", game.groupId));
            buttons = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.gdpDiscuss(), onClick);
        }
        if (DeploymentConfig.devDeployment) {
            ClickListener onClick = Link.createListener(Pages.SHOP, Args.compose("g", game.gameId));
            Widget shop = MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.gdpShop(), onClick);
            buttons = (buttons == null) ? shop : MsoyUI.createButtonPair(buttons, shop);
        }
        if (buttons != null) {
            setWidget(2, 0, buttons);
            getFlexCellFormatter().setRowSpan(0, 0, 3);
            getFlexCellFormatter().setRowSpan(1, 1, 2);
        }

        // note that they're playing the developer version if so
        if (Game.isDeveloperVersion(gameId)) {
            addText(_msgs.gdpDevVersion(), 3, "InDevTip");
        }

        _tabs = new StyledTabPanel();
        _tabs.addTabListener(this);
        addWidget(_tabs, 3, null);

        // add the about/instructions tab
        addTab(GameDetails.INSTRUCTIONS, _msgs.tabInstructions(), new InstructionsPanel(detail));

        // add comments tab
        if (detail.listedItem != null) {
            addTab(GameDetails.COMMENTS, _msgs.tabComments(),
                   new CommentsPanel(detail.listedItem.getType(), detail.listedItem.catalogId));
        }

        // add trophies tab, passing in the potentially negative gameId
        addTab(GameDetails.TROPHIES, _msgs.tabTrophies(), new GameTrophyPanel(gameId));

        // add top rankings tabs
        if (!CShell.isGuest()) {
            addTab(GameDetails.MYRANKINGS, _msgs.tabMyRankings(),
                   new TopRankingPanel(detail.gameId, true));
        }
        addTab(GameDetails.TOPRANKINGS, _msgs.tabTopRankings(),
               new TopRankingPanel(detail.gameId, false));

        // if we're the owner of the game or an admin, add the metrics tab
        if (detail.listedItem != null && ((detail.sourceItem != null
                && detail.sourceItem.ownerId == CShell.getMemberId()) || CShell.isAdmin())) {
            addTab(GameDetails.METRICS, _msgs.tabMetrics(), new GameMetricsPanel(detail));
            addTab(GameDetails.LOGS, _msgs.tabLogs(), new GameLogsPanel(gameId));
        }
    }

    // from interface TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // route tab selection through the URL
        GameDetails tabCode = getTabCode(tabIndex);
        if (tabCode == _seltab) {
            return true;
        }
        Link.go(Pages.GAMES, NaviUtil.gameDetail(_gameId, tabCode));
        return false;
    }

    // from interface TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // nada
    }

    protected void addTab (GameDetails ident, String title, Widget tab)
    {
        _tabs.add(tab, title);
        _tabmap.put(ident, _tabs.getWidgetCount() - 1);
    }

    protected void selectTab (GameDetails tab)
    {
        Integer tosel = _tabmap.get(tab);
        if (tosel == null) {
            _seltab = getTabCode(0);
            _tabs.selectTab(0);
        } else {
            _seltab = tab;
            _tabs.selectTab(tosel.intValue());
        }
    }

    protected GameDetails getTabCode (int tabIndex)
    {
        for (Map.Entry<GameDetails, Integer> entry : _tabmap.entrySet()) {
            if (entry.getValue() == tabIndex) {
                return entry.getKey();
            }
        }
        return GameDetails.INSTRUCTIONS;
    }

    protected StyledTabPanel _tabs;
    protected int _gameId;
    protected GameDetails _seltab;
    protected Map<GameDetails, Integer> _tabmap = new HashMap<GameDetails, Integer>();

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
