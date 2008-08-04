//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.threerings.gwt.ui.SmartTable;

/**
 * Displays informational bits about a game: number of players, average duration, number of games
 * played, etc.
 */
public class GameBitsPanel extends SmartTable
{
    public GameBitsPanel (int minPlayers, int maxPlayers, int avgTime, int gamesPlayed)
    {
        super("gameBits", 0, 0);

        int row = 0;
        if (maxPlayers == Integer.MAX_VALUE) {
            setText(row, 1, _msgs.bitsPlayersParty("" + minPlayers));
        } else if (minPlayers == maxPlayers) {
            setText(row, 1, _msgs.bitsPlayersSame("" + minPlayers));
        } else {
            setText(row, 1, _msgs.bitsPlayersFixed("" + minPlayers, "" + maxPlayers));
        }
        setText(row++, 0, _msgs.bitsPlayers(), 1, "Label");

        setText(row, 0, _msgs.bitsAvgDuration(), 1, "Label");
        setText(row++, 1, avgMinsLabel(Math.round(avgTime/60f)));

        if (gamesPlayed > 0) {
            setText(row, 0, _msgs.bitsGamesPlayed(), 1, "Label");
            setText(row++, 1, ""+gamesPlayed);
        }
    }

    protected static String avgMinsLabel (int avgMins)
    {
        return (avgMins > 1) ? _msgs.bitsMinutes(""+avgMins) : _msgs.bitsMinute();
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
