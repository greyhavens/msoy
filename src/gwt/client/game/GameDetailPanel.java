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
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.SimplePanel;
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
import client.util.MsoyUI;
import client.util.StyledTabPanel;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends VerticalPanel
    implements TabListener
{
    public static final String INSTRUCTIONS_TAB = "i";
    public static final String COMMENTS_TAB = "c";
    public static final String TROPHIES_TAB = "t";
    public static final String MYRANKINGS_TAB = "mr";
    public static final String TOPRANKINGS_TAB = "tr";
    public static final String METRICS_TAB = "m";

    public GameDetailPanel (Page page)
    {
        _page = page;
        _page.setPageTitle(CGame.msgs.gdpTitle());
        setStyleName("gameDetail");
    }

    public void setGame (final int gameId, final String tab)
    {
        if (_gameId == gameId) {
            selectTab(tab);
            return;
        }

        add(new Label(CGame.msgs.gdpLoading()));
        CGame.gamesvc.loadGameDetail(CGame.ident, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setGameDetail(gameId, (GameDetail)result);
                selectTab(tab);
            }
            public void onFailure (Throwable cause) {
                CGame.log("Failed to load detail [id=" +  _gameId + "]", cause);
                clear();
                add(new Label(CGame.serverError(cause)));
            }
        });
    }

    public void setGameDetail (int gameId, GameDetail detail)
    {
        clear();

        _gameId = gameId;
        _page.setPageTitle(CGame.msgs.gdpTitle(), detail.getGame().name);

        FlexTable top = new FlexTable();
        top.setCellPadding(0);
        top.setCellSpacing(0);

        int row = 0;
        FlexTable box = new FlexTable();
        box.setStyleName("Box");
        box.setCellPadding(0);
        box.setCellSpacing(0);
        box.getFlexCellFormatter().setStyleName(0, 0, "Name");
        box.setText(0, 0, detail.getGame().name);
        box.getFlexCellFormatter().setStyleName(1, 0, "Screenshot");
        MediaDesc shot = detail.getGame().shotMedia;
        if (shot == null) {
            shot = detail.getGame().getThumbnailMedia();
            CGame.log("No shot media, using " + shot + ".");
        } else {
            CGame.log("Using " + shot + ".");
        }
        box.setWidget(1, 0, new Image(shot.getMediaPath()));

        if (detail.listedItem != null) {
            box.setWidget(2, 0, WidgetUtil.makeShim(1, 5));
            box.getFlexCellFormatter().setHorizontalAlignment(3, 0, HasAlignment.ALIGN_CENTER);
            box.setWidget(3, 0, new ItemRating(
                              detail.listedItem, CGame.getMemberId(), detail.memberRating));
        }

        top.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
        top.setWidget(row, 0, box);

        VerticalPanel details = new VerticalPanel();
        details.setStyleName("Details");
        top.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
        top.setWidget(row, 1, details);

        FlexTable pbbox = new FlexTable();
        pbbox.setCellPadding(0);
        pbbox.setCellSpacing(0);
        pbbox.setText(0, 0, CGame.msgs.gdpPlay());
        pbbox.getFlexCellFormatter().setStyleName(0, 0, "PlayTitle");
        pbbox.getFlexCellFormatter().setColSpan(0, 0, 2);
        Button play;

        // if the game supports single-player play, it gets a "Quick Single" button
        if (detail.minPlayers == 1 && !detail.isPartyGame()) {
            addPlayButton(pbbox, 1, 0, "SinglePlay", CGame.msgs.gdpJustMe(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", ""+_gameId));
                }
            });
        }

        // if the game supports multiplayer play, it gets "Quick Multi" and "Custom Game" buttons
        if (detail.maxPlayers > 1) {
            addPlayButton(pbbox, 1, 1, "FriendPlay", CGame.msgs.gdpMyFriends(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "f", ""+_gameId));
                }
            });

            addPlayButton(pbbox, 3, 0, "AnyonePlay", CGame.msgs.gdpAnyone(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "m", ""+_gameId));
                }
            });

            addPlayButton(pbbox, 3, 1, "CustomPlay", CGame.msgs.gdpCustom(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "l", ""+_gameId));
                }
            });
        }

        top.getFlexCellFormatter().setVerticalAlignment(row, 2, HasAlignment.ALIGN_TOP);
        top.setWidget(row++, 2, pbbox);
        add(top);

        SimplePanel cbox = new SimplePanel();
        cbox.setStyleName("Creator");
        if (detail.creator != null) {
            CreatorLabel creator = new CreatorLabel();
            creator.setMember(detail.creator);
            cbox.add(creator);
        }
        details.add(cbox);

        details.add(new Label(ItemUtil.getDescription(detail.getGame())));
        details.add(WidgetUtil.makeShim(1, 5));

        // set up the game info table
        float avg = detail.playerMinutes / (float)detail.playerGames;
        int avgMins = Math.max(1, Math.round(avg));

        FlexTable info = new FlexTable();
        info.setCellPadding(0);
        info.setCellSpacing(0);

        int irow = 0;
        info.setText(irow, 0, CGame.msgs.gdpInfoTitle());
        info.getFlexCellFormatter().setStyleName(irow++, 0, "InfoTitle");

        String[] ilabels = {
            CGame.msgs.gdpPlayers(), CGame.msgs.gdpAvgDuration(), CGame.msgs.gdpGamesPlayed()
        };
        String playersStr;
        if (detail.isPartyGame()) {
            playersStr = CGame.msgs.gdpPlayersParty("" + detail.minPlayers);
        } else if (detail.minPlayers == detail.maxPlayers) {
            playersStr = CGame.msgs.gdpPlayersSame("" + detail.minPlayers);
        } else {
            playersStr = CGame.msgs.gdpPlayersFixed("" + detail.minPlayers, "" + detail.maxPlayers);
        }
        String[] ivalues = {
            playersStr,
            (avgMins > 1) ? CGame.msgs.gdpMinutes(""+avgMins) : CGame.msgs.gdpMinute(),
            Integer.toString(detail.playerGames),
        };
        for (int ii = 0; ii < ilabels.length; ii++) {
            info.setText(irow, 0, ilabels[ii]);
            info.getFlexCellFormatter().setStyleName(irow, 0, "InfoLabel");
            info.setText(irow++, 1, ivalues[ii]);
        }
        details.add(info);

        // note that they're playing the developer version if so
        if (_gameId < 0) {
            top.setText(row, 0, "");
            top.setText(row, 1, CGame.msgs.gdpDevVersion());
            top.getFlexCellFormatter().setColSpan(row, 1, top.getCellCount(0));
            top.getFlexCellFormatter().setStyleName(row++, 1, "InDevTip");
        }

        _tabs = new StyledTabPanel();
        _tabs.addTabListener(this);
        add(_tabs);

        // add the about/instructions tab
        addTab(INSTRUCTIONS_TAB, CGame.msgs.tabInstructions(), new InstructionsPanel(detail));

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

        // if we're the owner of the game or an admin, add the metrics tab
        if ((detail.sourceItem != null && detail.sourceItem.ownerId == CGame.getMemberId()) ||
            CGame.isAdmin()) {
            addTab(METRICS_TAB, CGame.msgs.tabMetrics(), new GameMetricsPanel(detail));
        }
    }

    protected void addPlayButton (FlexTable table, int row, int column,
                                  String styleName, String tip, ClickListener onClick)
    {
        Button play = new Button("", onClick);
        play.setStyleName("PlayButton");
        play.addStyleName(styleName);
        table.setWidget(row, column, play);
        table.getFlexCellFormatter().setStyleName(row, column, "PlayCell");
        table.setText(row+1, column, tip);
        table.getFlexCellFormatter().setStyleName(row+1, column, "PlayLabel");
    }

    // from interface TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // route tab selection through the URL
        String tabCode = getTabCode(tabIndex);
        if (!tabCode.equals(_seltab)) {
            Application.go(Page.GAME, Args.compose("d", ""+_gameId, tabCode));
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
