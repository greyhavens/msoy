//
// $Id$

package client.game;

import com.threerings.gwt.ui.SmartTable;

/**
 * Displays informational bits about a game: number of players, average duration, number of games
 * played, etc.
 */
public class GameBitsPanel extends SmartTable
{
    public GameBitsPanel (String title, byte genre, int minPlayers, int maxPlayers,
                          int avgTime, int gamesPlayed)
    {
        super("gameBits", 0, 0);

        int row = 0;
        if (title != null) {
            setText(row++, 0, title, 1, "Title");
        }

        setText(row, 0, CGame.msgs.bitsGenre(), 1, "Label");
        setText(row++, 1, CGame.dmsgs.getString("genre" + genre));

        if (maxPlayers == Integer.MAX_VALUE) {
            setText(row, 1, CGame.msgs.bitsPlayersParty("" + minPlayers));
        } else if (minPlayers == maxPlayers) {
            setText(row, 1, CGame.msgs.bitsPlayersSame("" + minPlayers));
        } else {
            setText(row, 1, CGame.msgs.bitsPlayersFixed("" + minPlayers, "" + maxPlayers));
        }
        setText(row++, 0, CGame.msgs.bitsPlayers(), 1, "Label");

        setText(row, 0, CGame.msgs.bitsAvgDuration(), 1, "Label");
        setText(row++, 1, avgMinsLabel(avgTime));

        if (gamesPlayed > 0) {
            setText(row, 0, CGame.msgs.bitsGamesPlayed(), 1, "Label");
            setText(row++, 1, ""+gamesPlayed);
        }
    }

    protected static String avgMinsLabel (int avgMins)
    {
        return (avgMins > 1) ? CGame.msgs.bitsMinutes(""+avgMins) : CGame.msgs.bitsMinute();
    }
}
