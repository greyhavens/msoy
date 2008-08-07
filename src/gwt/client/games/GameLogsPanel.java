//
// $Id: GameLogsPanel.java 9296 2008-05-28 14:51:30Z mdb $

package client.games;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.game.gwt.GameLogs;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.ui.MsoyUI;
import client.util.ServiceUtil;

/**
 * Displays the server-side logs for a particular game, if any.
 */
public class GameLogsPanel extends VerticalPanel
{
    public GameLogsPanel (int gameId)
    {
        setStyleName("gameLogs");
        _gameId = gameId;
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _logs != null) {
            return;
        }

        add(MsoyUI.createLabel(_msgs.glpLoading(), "Header"));
        _gamesvc.loadGameLogs(_gameId, new AsyncCallback<GameLogs>() {
            public void onSuccess (GameLogs logs) {
                gotLogs(logs);
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
            add(new Label(_msgs.glpNoLogs()));
            return;
        }

        add(MsoyUI.createLabel(_msgs.glpLogsHeader(), "Header"));

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
            String href = "/gamelogs?gameId=" + _gameId + "&logId=" + logs.logIds[ii];
            String label = _dfmt.format(logs.logTimes[ii]);

            table.setWidget(row, col, new HTML(
                                "<a target='_blank' href='" + href + "'>" + label + "</a>"));
            row ++;
            if (row * (TABLE_COLUMNS - col - 1) >= (logs.logIds.length - ii)) {
                row = 0;
                col ++;
            }
        }

        add(table);
    }

    protected int _gameId;
    protected GameLogs _logs;

    protected static final SimpleDateFormat _dfmt = new SimpleDateFormat("h:mm aa, MMMMM dd, yyyy");
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);

    /** The number of columns in the log table. */
    protected static final int TABLE_COLUMNS = 3;
}
