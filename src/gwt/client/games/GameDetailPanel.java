//
// $Id$

package client.games;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.item.data.all.Game;

import client.item.ItemRating;
import client.msgs.CommentsPanel;
import client.shell.Page;
import client.ui.StyledTabPanel;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NaviUtil;
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
            CGames.gamesvc.loadGameDetail(CGames.ident, gameId, new MsoyCallback<GameDetail>() {
                public void onSuccess (GameDetail detail) {
                    setGameDetail(gameId, detail);
                    selectTab(tab);
                }
            });
        }
    }

    public void setGameDetail (int gameId, GameDetail detail)
    {
        // Note: the gameId may be the negative original gameId, but GameDetail's id is never
        // negative to match
        _gameId = gameId;
        CGames.frame.setTitle(detail.getGame().name);

        Game game = detail.getGame();
        VerticalPanel shot = new VerticalPanel();
        shot.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        shot.add(new ThumbBox(game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT, null));
        if (detail.listedItem != null) {
            shot.add(WidgetUtil.makeShim(5, 5));
            shot.add(new ItemRating(detail.listedItem, detail.memberItemInfo, false));
        }
        setWidget(0, 0, shot);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        setWidget(0, 1, new GameNamePanel(
                      game.name, game.genre, detail.creator, game.description), 2, null);
        setWidget(1, 0, new GameBitsPanel(detail.minPlayers, detail.maxPlayers,
                                          detail.averageDuration, detail.gamesPlayed));
        setWidget(1, 1, new PlayPanel(_gameId, detail.minPlayers, detail.maxPlayers,
                                      detail.playingNow), 1, "Play");

        // note that they're playing the developer version if so
        if (Game.isDeveloperVersion(gameId)) {
            addText(CGames.msgs.gdpDevVersion(), 3, "InDevTip");
        }

        _tabs = new StyledTabPanel();
        _tabs.addTabListener(this);
        addWidget(_tabs, 3, null);

        // add the about/instructions tab
        addTab(GameDetails.INSTRUCTIONS,
               CGames.msgs.tabInstructions(), new InstructionsPanel(detail));

        // add comments tab
        if (detail.listedItem != null) {
            addTab(GameDetails.COMMENTS, CGames.msgs.tabComments(),
                   new CommentsPanel(detail.listedItem.getType(), detail.listedItem.catalogId));
        }

        // add trophies tab, passing in the potentially negative gameId
        addTab(GameDetails.TROPHIES, CGames.msgs.tabTrophies(), new GameTrophyPanel(gameId));

        // add top rankings tabs
        if (!CGames.isGuest()) {
            addTab(GameDetails.MYRANKINGS, CGames.msgs.tabMyRankings(),
                   new TopRankingPanel(detail.gameId, true));
        }
        addTab(GameDetails.TOPRANKINGS, CGames.msgs.tabTopRankings(),
               new TopRankingPanel(detail.gameId, false));

        // if we're the owner of the game or an admin, add the metrics tab
        if ((detail.sourceItem != null && detail.sourceItem.ownerId == CGames.getMemberId()) ||
            CGames.isAdmin()) {
            addTab(GameDetails.METRICS, CGames.msgs.tabMetrics(), new GameMetricsPanel(detail));
            addTab(GameDetails.LOGS, CGames.msgs.tabLogs(), new GameLogsPanel(detail));
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
        Link.go(Page.GAMES, NaviUtil.gameDetail(_gameId, tabCode));
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
}
