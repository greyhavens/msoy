//
// $Id$

package client.games;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.game.gwt.GameLogs;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.shell.CShell;
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
        if (!visible || _gotLogs) {
            return;
        }

        add(MsoyUI.createLabel(_msgs.glpLoading(), "Header"));
        _gamesvc.loadGameLogs(_gameId, new AsyncCallback<GameLogs>() {
            public void onSuccess (GameLogs logs) {
                gotLogs(logs);
            }
            public void onFailure (Throwable caught) {
                CShell.log("loadGameLogs failed", caught);
                add(MsoyUI.createLabel(CShell.serverError(caught), "Header"));
            }
        });
    }

    protected void gotLogs (GameLogs logs)
    {
        _gotLogs = true;

        clear();

        if (logs.logIds.length == 0) {
            add(new Label(_msgs.glpNoLogs()));
            return;
        }

        // Sort logs here so database doesn't have to
        TreeMap<Date, Integer> sortedLogs = new TreeMap<Date, Integer>();
        for (int ii = 0; ii < logs.logIds.length; ++ii) {
            sortedLogs.put(logs.logTimes[ii], logs.logIds[ii]);
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
        int ii = 0;
        for (Map.Entry<Date, Integer> log : sortedLogs.entrySet()) {
            String href = "/gamelogs?gameId=" + _gameId + "&logId=" + log.getValue();
            String label = MsoyUI.formatDateTime(log.getKey());

            table.setWidget(
                row, col, MsoyUI.createHTML("<a href='" + href + "'>" + label + "</a>", null));
            row ++;
            if (row * (TABLE_COLUMNS - col - 1) >= (sortedLogs.size() - ii)) {
                row = 0;
                col ++;
            }
            ii ++;
        }

        add(table);
    }

    protected int _gameId;
    protected boolean _gotLogs;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);

    /** The number of columns in the log table. */
    protected static final int TABLE_COLUMNS = 3;
}
