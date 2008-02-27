//
// $Id$

package client.games;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.GameDetail;

import client.item.ItemRating;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.Frame;
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

    public GameDetailPanel ()
    {
        setStyleName("gameDetail");
    }

    public void setGame (final int gameId, final String tab)
    {
        if (_gameId == gameId) {
            selectTab(tab);
            return;
        }

        add(new Label(CGames.msgs.gdpLoading()));
        CGames.gamesvc.loadGameDetail(CGames.ident, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setGameDetail(gameId, (GameDetail)result);
                selectTab(tab);
            }
            public void onFailure (Throwable cause) {
                CGames.log("Failed to load detail [id=" +  _gameId + "]", cause);
                clear();
                add(new Label(CGames.serverError(cause)));
            }
        });
    }

    public void setGameDetail (int gameId, GameDetail detail)
    {
        clear();

        _gameId = gameId;
        Frame.setTitle(detail.getGame().name);

        int row = 0;
        SmartTable top = new SmartTable(0, 0);
        top.setWidget(row, 0, MsoyUI.createBackArrow(), 1, "Up");

        SmartTable box = new SmartTable("Box", 0, 0);
        box.setText(0, 0, detail.getGame().name, 1, "Name");
        MediaDesc shot = detail.getGame().shotMedia;
        if (shot == null) {
            shot = detail.getGame().getThumbnailMedia();
            CGames.log("No shot media, using " + shot + ".");
        } else {
            CGames.log("Using " + shot + ".");
        }
        box.setWidget(1, 0, new Image(shot.getMediaPath()), 1, "Screenshot");
        if (detail.listedItem != null) {
            box.setWidget(2, 0, WidgetUtil.makeShim(1, 5));
            box.getFlexCellFormatter().setHorizontalAlignment(3, 0, HasAlignment.ALIGN_CENTER);
            box.setWidget(3, 0, new ItemRating(detail.listedItem, CGames.getMemberId(),
                                               detail.memberRating, false));
        }
        top.setWidget(row, 1, box);

        VerticalPanel details = new VerticalPanel();
        top.setWidget(row, 2, details, 1, "Details");

        top.setWidget(row, 3, new PlayPanel(_gameId, detail.minPlayers, detail.maxPlayers));
        for (int ii = 0; ii < top.getCellCount(0); ii++) {
            top.getFlexCellFormatter().setVerticalAlignment(row, ii, HasAlignment.ALIGN_TOP);
        }
        row++;
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
        details.add(new GameBitsPanel(CGames.msgs.gdpInfoTitle(), detail.getGame().genre,
                                      detail.minPlayers, detail.maxPlayers,
                                      detail.getAverageDuration(),
                                      detail.singlePlayerGames + detail.multiPlayerGames));

        // note that they're playing the developer version if so
        if (_gameId < 0) {
            top.getFlexCellFormatter().setRowSpan(0, 0, 2);
            top.getFlexCellFormatter().setRowSpan(0, 1, 2);
            top.setText(row++, 0, CGames.msgs.gdpDevVersion(), top.getCellCount(0)-2, "InDevTip");
        }

        _tabs = new StyledTabPanel();
        _tabs.addTabListener(this);
        add(_tabs);

        // add the about/instructions tab
        addTab(INSTRUCTIONS_TAB, CGames.msgs.tabInstructions(), new InstructionsPanel(detail));

        // add comments tab
        if (detail.listedItem != null) {
            addTab(COMMENTS_TAB, CGames.msgs.tabComments(),
                   new CommentsPanel(detail.listedItem.getType(), detail.listedItem.catalogId));
        }

        // add trophies tab
        addTab(TROPHIES_TAB, CGames.msgs.tabTrophies(), new GameTrophyPanel(detail.gameId));

        // add top rankings tabs
        if (CGames.getMemberId() != 0) {
            addTab(MYRANKINGS_TAB, CGames.msgs.tabMyRankings(),
                   new TopRankingPanel(detail.gameId, true));
        }
        addTab(TOPRANKINGS_TAB, CGames.msgs.tabTopRankings(),
               new TopRankingPanel(detail.gameId, false));

        // if we're the owner of the game or an admin, add the metrics tab
        if ((detail.sourceItem != null && detail.sourceItem.ownerId == CGames.getMemberId()) ||
            CGames.isAdmin()) {
            addTab(METRICS_TAB, CGames.msgs.tabMetrics(), new GameMetricsPanel(detail));
        }
    }

    // from interface TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // route tab selection through the URL
        String tabCode = getTabCode(tabIndex);
        if (!tabCode.equals(_seltab)) {
            Application.go(Page.GAMES, Args.compose("d", ""+_gameId, tabCode));
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

    protected StyledTabPanel _tabs;
    protected int _gameId;
    protected String _seltab;
    protected HashMap _tabmap = new HashMap();
}
