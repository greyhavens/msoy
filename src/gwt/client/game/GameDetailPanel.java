//
// $Id$

package client.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.GameDetail;

import client.item.ItemRating;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.Page;
import client.util.CreatorLabel;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.StyledTabPanel;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends FlexTable
    implements TabListener
{
    public static final String COMMENTS_TAB = "c";
    public static final String TROPHIES_TAB = "t";
    public static final String MYRANKINGS_TAB = "mr";
    public static final String TOPRANKINGS_TAB = "tr";

    public GameDetailPanel (Page page)
    {
        _page = page;
        _page.setPageTitle(CGame.msgs.gdpTitle());
        setStyleName("gameDetail");
        setCellPadding(0);
        setCellSpacing(0);
        setText(0, 0, CGame.msgs.gdpLoading());
    }

    public void setGame (final int gameId, final String tab)
    {
        if (_gameId == gameId) {
            selectTab(tab);
            return;
        }

        CGame.gamesvc.loadGameDetail(CGame.ident, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setGameDetail(gameId, (GameDetail)result);
                selectTab(tab);
            }
            public void onFailure (Throwable cause) {
                CGame.log("Failed to load detail [id=" +  _gameId + "]", cause);
                setText(0, 0, CGame.serverError(cause));
            }
        });
    }

    public void setGameDetail (int gameId, GameDetail detail)
    {
        _gameId = gameId;
        _page.setPageTitle(CGame.msgs.gdpTitle(), detail.getGame().name);

        int row = 0;
        FlexTable box = new FlexTable();
        box.setStyleName("Box");
        box.setCellPadding(0);
        box.setCellSpacing(0);
        box.getFlexCellFormatter().setStyleName(0, 0, "Name");
        box.setText(0, 0, detail.getGame().name);
        box.getFlexCellFormatter().setStyleName(1, 0, "Logo");
        box.setWidget(1, 0, MediaUtil.createMediaView(
                          detail.getGame().getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE));
        setWidget(row, 0, box);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        VerticalPanel details = new VerticalPanel();
        details.setStyleName("Details");
        setWidget(row, 1, details);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

        VerticalPanel playButtons = new VerticalPanel();
        playButtons.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        playButtons.setSpacing(15);
        Button play;
        playButtons.add(play = new Button("", new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.GAME, Args.compose("s", _gameId));
            }
        }));
        play.setStyleName("PlayButton");
        if (detail.maxPlayers > 1) {
            playButtons.add(MsoyUI.createLabel(CGame.msgs.gdpOrWithFriends(), "OrFriends"));
            playButtons.add(new Button(CGame.msgs.gdpMultiplayer(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GAME, "" + _gameId);
                }
            }));
        }
        setWidget(row++, 2, playButtons);

        if (detail.listedItem != null) {
            details.add(new ItemRating(detail.listedItem, CGame.getMemberId(),
                                       detail.memberRating));
            details.add(WidgetUtil.makeShim(1, 5));
        }

        if (detail.creator != null) {
            CreatorLabel creator = new CreatorLabel();
            creator.setMember(detail.creator);
            details.add(creator);
        }

        details.add(new Label(ItemUtil.getDescription(detail.getGame())));
        details.add(WidgetUtil.makeShim(1, 15));

        // set up the game info table
        float avg = detail.playerMinutes / (float)detail.playerGames;
        int avgMins = (int)Math.floor(avg);
        int avgSecs = (int)Math.floor(avg * 60) % 60;

        FlexTable info = new FlexTable();
        info.setCellPadding(0);
        info.setCellSpacing(0);
        String[] ilabels = {
            CGame.msgs.gdpPlayers(), CGame.msgs.gdpGamesPlayed(), CGame.msgs.gdpAvgDuration()
        };
        String[] ivalues = {
            (detail.maxPlayers == Integer.MAX_VALUE ?
             CGame.msgs.gdpPlayersParty("" + detail.minPlayers) :
             CGame.msgs.gdpPlayersFixed("" + detail.minPlayers, "" + detail.maxPlayers)),
            Integer.toString(detail.playerGames),
            avgMins + ":" + avgSecs,
        };
        for (int ii = 0; ii < ilabels.length; ii++) {
            info.setText(ii, 0, ilabels[ii]);
            info.getFlexCellFormatter().setStyleName(ii, 0, "InfoLabel");
            info.setText(ii, 1, ivalues[ii]);
        }
        details.add(info);

        // note that they're playing the developer version if so
        if (_gameId < 0) {
            setText(row, 0, "");
            setText(row, 1, CGame.msgs.gdpDevVersion());
            getFlexCellFormatter().setColSpan(row, 1, getCellCount(0));
            getFlexCellFormatter().setStyleName(row++, 1, "tipLabel");
        }

        _tabs = new StyledTabPanel();
        _tabs.addTabListener(this);
        setWidget(row, 0, _tabs);
        getFlexCellFormatter().setColSpan(row++, 0, getCellCount(0));

        // TODO: add screen shots tab
        // TODO: add instructions tab

        // add comments tab
        if (detail.listedItem != null) {
            addTab(COMMENTS_TAB, CGame.msgs.tabComments(),
                   new CommentsPanel(detail.listedItem.getType(), detail.listedItem.catalogId));
        }

        // add trophies tab
        addTab(TROPHIES_TAB, CGame.msgs.tabTrophies(), new GameTrophyPanel(detail.gameId));

        // add top rankings tabs
        if (CGame.getMemberId() != 0) {
            addTab(MYRANKINGS_TAB, CGame.msgs.tabMyRankings(),
                   new TopRankingPanel(detail.gameId, true));
        }
        addTab(TOPRANKINGS_TAB, CGame.msgs.tabTopRankings(),
               new TopRankingPanel(detail.gameId, false));
    }

    // from interface TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // route tab selection through the URL
        String tabCode = getTabCode(tabIndex);
        if (!tabCode.equals(_seltab)) {
            Application.go(Page.GAME, Args.compose(new String[] { "d", ""+ _gameId, tabCode }));
            return false;
        } else {
            return true;
        }
    }

    // from interface TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // nada
    }

    protected void addTab (String ident, String title, Widget tab)
    {
        _tabs.add(tab, title);
        _tabmap.put(ident, new Integer(_tabs.getWidgetCount()-1));
    }

    protected void selectTab (String tab)
    {
        Integer tosel = (Integer)_tabmap.get(tab);
        if (tosel == null) {
            _seltab = getTabCode(0);
            _tabs.selectTab(0);
        } else {
            _seltab = tab;
            _tabs.selectTab(tosel.intValue());
        }
    }

    protected String getTabCode (int tabIndex)
    {
        for (Iterator iter = _tabmap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((Integer)entry.getValue()).intValue() == tabIndex) {
                return (String)entry.getKey();
            }
        }
        return "";
    }

    protected Page _page;
    protected StyledTabPanel _tabs;
    protected int _gameId;
    protected String _seltab;
    protected HashMap _tabmap = new HashMap();
}
