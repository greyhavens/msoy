//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.util.Link;

/**
 * Displays informational bits about a game: number of players, average duration, number of games
 * played, etc.
 */
public class GameBitsPanel extends SmartTable
{
    public GameBitsPanel (GameDetail detail)
    {
        super("gameBits", 0, 0);

        int row = 0;

        if (detail.maxPlayers == Integer.MAX_VALUE) {
            setText(row, 1, _msgs.bitsPlayersParty(""+detail.minPlayers));
        } else if (detail.minPlayers == detail.maxPlayers) {
            setText(row, 1, _msgs.bitsPlayersSame(""+detail.minPlayers));
        } else {
            setText(row, 1, _msgs.bitsPlayersFixed(""+detail.minPlayers, ""+detail.maxPlayers));
        }
        setText(row++, 0, _msgs.bitsPlayers(), 1, "Label");

        setText(row, 0, _msgs.bitsAvgDuration(), 1, "Label");
        setText(row++, 1, avgMinsLabel(Math.round(detail.metrics.averageDuration/60f)));

        if (detail.metrics.gamesPlayed > 0) {
            setText(row, 0, _msgs.bitsGamesPlayed(), 1, "Label");
            setText(row++, 1, detail.metrics.gamesPlayed, 1);
        }

        if (CShell.getMemberId() == detail.info.creator.getMemberId() || CShell.isSupport()) {
            setWidget(row++, 0, Link.create(_msgs.bitsEdit(), Pages.EDGAMES, "e",
                detail.info.gameId), 2);
        }
    }

    protected static String avgMinsLabel (int avgMins)
    {
        return (avgMins > 1) ? _msgs.bitsMinutes(""+avgMins) : _msgs.bitsMinute();
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
