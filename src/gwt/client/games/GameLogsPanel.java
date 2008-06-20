//
// $Id: GameLogsPanel.java 9296 2008-05-28 14:51:30Z mdb $

package client.games;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.GameLogs;

import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays the server-side logs for a particular game, if any.
 */
public class GameLogsPanel extends VerticalPanel
{
    public GameLogsPanel (GameDetail detail)
    {
        setStyleName("gameLogs");
        _detail = detail;
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _logs != null) {
            return;
        }

        add(MsoyUI.createLabel(CGames.msgs.gmpLoading(), "Header"));
        CGames.gamesvc.loadGameLogs(CGames.ident, _detail.gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotLogs((GameLogs)result);
            }
            public void onFailure (Throwable caught) {
                CGames.log("loadGameLogs failed", caught);
                add(MsoyUI.createLabel(CGames.serverError(caught), "Header"));
            }
        });
    }

    protected void gotLogs (GameLogs logs)
    {
        _logs = logs;
        clear();

        if (logs.logIds.length == 0) {
            add(new Label(CGames.msgs.glpNoLogs()));
            return;
        }

        add(MsoyUI.createLabel(CGames.msgs.glpLogsHeader(), "Header"));

        /**
         * Let's tabulate like so:
         *    1 4 6
         *    2 5 7
         *    3
         */
        SmartTable table = new SmartTable("Table", 3, 0);

        int row = 0;
        int col = 0;
        for (int ii = 0; ii < logs.logIds.length; ii ++) {
            String href = "/gamelogs?gameId=" + _detail.gameId + "&logId=" + logs.logIds[ii];
            String label = DATE_FORMAT.format(logs.logTimes[ii]);

            table.setWidget(row, col, new HTML(
                                "<a target='_blank' href='" + href + "'>" + label + "</a>"));
            row ++;
            if (row * (TABLE_COLUMNS - col) >= (logs.logIds.length - ii)) {
                row = 0;
                col ++;
            }
        }

        add(table);
    }

    protected GameDetail _detail;
    protected GameLogs _logs;

    /** Used to format the most recent post date. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("h:mm aa, MMMMM dd, yyyy");

    /** The number of columns in the log table. */
    protected static final int TABLE_COLUMNS = 3;
}
