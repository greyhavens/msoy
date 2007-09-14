//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.GameDetail;

import client.shell.Page;
import client.util.MediaUtil;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends FlexTable
{
    public GameDetailPanel (int gameId)
    {
        setStyleName("gameDetail");
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

    public void setGameDetail (GameDetail detail)
    {
        setText(0, 0, detail.getName());
        getFlexCellFormatter().setColSpan(0, 0, 3);
        getFlexCellFormatter().setStyleName(0, 0, "Title");

        float avg = detail.playerMinutes / (float)detail.playerGames;
        int avgMins = (int)Math.floor(avg);
        int avgSecs = (int)Math.floor(avg * 60) % 60;

        int row = 1;
        // this row coincides with the logo, so we have to slide it over one
        setText(row, 1, "Average duration:");
        setText(row++, 2, avgMins + ":" + avgSecs);

        if (CGame.isAdmin()) {
            setText(row, 0, "Player games:");
            setText(row++, 1, Integer.toString(detail.playerGames));

            setText(row, 0, "Player minutes:");
            setText(row++, 1, Integer.toString(detail.playerMinutes));

            setText(row, 0, "Abuse factor:");
            setText(row++, 1, Float.toString(detail.abuseFactor));

            setText(row, 0, "Last abuse recalc:");
            setText(row++, 1, Integer.toString(detail.lastAbuseRecalc));
        }

        Widget preview = MediaUtil.createMediaView(
            detail.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        setWidget(1, 0, preview);
        getFlexCellFormatter().setRowSpan(1, 0, row-1);
    }

    protected Page _page;
}
