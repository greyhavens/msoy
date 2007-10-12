//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.GameDetail;

import client.item.CreatorLabel;
import client.item.ItemRating;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.Page;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.StyledTabPanel;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends FlexTable
{
    public GameDetailPanel (Page page, final int gameId, final String tab)
    {
        _page = page;
        _page.setPageTitle(CGame.msgs.gdpTitle());
        setStyleName("gameDetail");
        setCellPadding(0);
        setCellSpacing(0);
        setText(0, 0, CGame.msgs.gdpLoading());

        CGame.gamesvc.loadGameDetail(CGame.ident, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setGameDetail((GameDetail)result, gameId, tab);
            }
            public void onFailure (Throwable cause) {
                setText(0, 0, CGame.serverError(cause));
            }
        });
    }

    public void setGameDetail (GameDetail detail, final int origGameId, String tab)
    {
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
                Application.go(Page.GAME, Args.compose("s", origGameId));
            }
        }));
        play.setStyleName("PlayButton");
        if (detail.maxPlayers > 1) {
            playButtons.add(MsoyUI.createLabel(CGame.msgs.gdpOrWithFriends(), "OrFriends"));
            playButtons.add(new Button(CGame.msgs.gdpMultiplayer(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GAME, "" + origGameId);
                }
            }));
        }
        setWidget(row++, 2, playButtons);

        if (detail.listedItem != null) {
            details.add(new ItemRating(detail.listedItem, CGame.getMemberId(),
                                       detail.memberRating));
            details.add(WidgetUtil.makeShim(1, 5));
        }

        CreatorLabel creator = new CreatorLabel();
        creator.setMember(detail.creator);
        details.add(creator);

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
        if (origGameId < 0) {
            setText(row, 0, "");
            setText(row, 1, CGame.msgs.gdpDevVersion());
            getFlexCellFormatter().setColSpan(row, 1, getCellCount(0));
            getFlexCellFormatter().setStyleName(row++, 1, "tipLabel");
        }

        StyledTabPanel tabs = new StyledTabPanel();
        setWidget(row, 0, tabs);
        getFlexCellFormatter().setColSpan(row++, 0, getCellCount(0));

        // TODO: add screen shots tab
        // TODO: add instructions tab

        // add comments tab
        if (detail.listedItem != null) {
            tabs.add(new CommentsPanel(detail.listedItem.getType(),
                                       detail.listedItem.catalogId), CGame.msgs.tabComments());
            if (tab.equals("")) {
                tabs.selectTab(tabs.getWidgetCount()-1);
            }
        }

        // add trophies tab
        tabs.add(new GameTrophyPanel(detail.gameId), CGame.msgs.tabTrophies());
        if (tab.equals("t")) {
            tabs.selectTab(tabs.getWidgetCount()-1);
        }
    }

    protected Page _page;
}
