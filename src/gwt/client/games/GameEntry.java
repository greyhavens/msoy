//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.Args;
import client.shell.Page;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays some info on a game.
 */
public class GameEntry extends SmartTable
{
    public GameEntry (final GameInfo game)
    {
        setStyleName("gameEntry");

        ClickListener onClick = new ClickListener() {
            public void onClick (Widget widget) {
                Link.go(Page.GAMES, Args.compose("d", game.gameId));
            }
        };
        setWidget(0, 0, MediaUtil.createMediaView(
                      game.getThumbMedia(), MediaDesc.THUMBNAIL_SIZE, onClick), 1, "Thumb");

        setWidget(0, 1, MsoyUI.createActionLabel(game.name, onClick), 1, "Name");

        setText(1, 0, truncate(game.description), 1, "Descrip");

        String players = (game.playersOnline == 0) ? "" :
            (game.playersOnline == 1 ? CGames.msgs.genrePlayer() :
             CGames.msgs.genrePlayers(""+game.playersOnline));
        setText(2, 0, players, 1, "Players");
        getFlexCellFormatter().setRowSpan(0, 0, 3);
    }

    protected static String truncate (String descrip)
    {
        if (descrip.length() <= MAX_DESCRIP_LENGTH) {
            return descrip;
        }
        for (int ii = 0; ii < MAX_DESCRIP_LENGTH; ii++) {
            char c = descrip.charAt(ii);
            if (c == '.' || c == '!') {
                return descrip.substring(0, ii+1);
            }
        }
        return descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 50;
}
