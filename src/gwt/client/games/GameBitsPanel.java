//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.util.Link;

/**
 * Displays informational bits about a game: number of players, average duration, number of games
 * played, etc.
 */
public class GameBitsPanel extends SmartTable
{
    /**
     * @param gamesPlayed If > 0 will display the total # of games played
     * @param originalItemId If > 0 and player is support, show link to original item
     * @param groupId If > 0 will display a link to the game's Whirled
     */
    public GameBitsPanel (int minPlayers, int maxPlayers, int avgTime, int gamesPlayed,
                          int originalItemId)
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

        if (originalItemId != 0 && CShell.isSupport()) {
            String args = Args.compose("d", Item.GAME, originalItemId);
            setWidget(row++, 0, Link.create(_msgs.bitsSeeOriginal(), Pages.STUFF, args), 2, null);
        }
    }

    protected static String avgMinsLabel (int avgMins)
    {
        return (avgMins > 1) ? _msgs.bitsMinutes(""+avgMins) : _msgs.bitsMinute();
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
