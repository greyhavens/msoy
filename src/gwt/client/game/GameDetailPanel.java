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
import client.shell.CommentsPanel;
import client.shell.Page;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.StyledTabPanel;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends FlexTable
{
    public GameDetailPanel (int gameId)
    {
        setStyleName("gameDetail");
        setCellPadding(0);
        setCellSpacing(0);
        setText(0, 0, "Loading game details...");
        CGame.gamesvc.loadGameDetail(CGame.ident, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setGameDetail((GameDetail)result);
            }
            public void onFailure (Throwable cause) {
                setText(0, 0, CGame.serverError(cause));
            }
        });
    }

    public void setGameDetail (final GameDetail detail)
    {
        FlexTable box = new FlexTable();
        box.setStyleName("Box");
        box.setCellPadding(0);
        box.setCellSpacing(0);
        box.getFlexCellFormatter().setStyleName(0, 0, "Name");
        box.setText(0, 0, detail.getGame().name);
        box.getFlexCellFormatter().setStyleName(1, 0, "Logo");
        box.setWidget(1, 0, MediaUtil.createMediaView(
                          detail.getGame().getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE));
        box.setWidget(2, 0, new Button(CGame.cmsgs.detailPlay(), new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.GAME, "" + detail.gameId);
            }
        }));
        box.getFlexCellFormatter().setStyleName(2, 0, "Play");
        setWidget(0, 0, box);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        VerticalPanel details = new VerticalPanel();
        details.setStyleName("Details");
        setWidget(0, 1, details);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

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

        FlexTable info = new FlexTable();
        info.setCellPadding(0);
        info.setCellSpacing(0);
        int row = 0;
        details.add(info);

        float avg = detail.playerMinutes / (float)detail.playerGames;
        int avgMins = (int)Math.floor(avg);
        int avgSecs = (int)Math.floor(avg * 60) % 60;
        info.setText(row, 0, "Average duration:");
        info.setText(row++, 1, avgMins + ":" + avgSecs);

        if (CGame.isAdmin()) {
            info.setText(row, 0, "Player games:");
            info.setText(row++, 1, Integer.toString(detail.playerGames));

            info.setText(row, 0, "Player minutes:");
            info.setText(row++, 1, Integer.toString(detail.playerMinutes));

            info.setText(row, 0, "Abuse factor:");
            info.setText(row++, 1, Float.toString(detail.abuseFactor));

            info.setText(row, 0, "Last abuse recalc:");
            info.setText(row++, 1, Integer.toString(detail.lastAbuseRecalc));
        }

        if (detail.listedItem != null) {
            StyledTabPanel tabs = new StyledTabPanel();
            setWidget(2, 0, tabs);
            tabs.add(new CommentsPanel(detail.listedItem.getType(),
                                       detail.listedItem.catalogId), "Comments");
            tabs.selectTab(0);
            getFlexCellFormatter().setColSpan(2, 0, 2);
        }

        // TODO: add screen shots
        // TODO: add player's trophies
    }

    protected Page _page;
}
