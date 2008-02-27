//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;

/**
 * Does something extraordinary.
 */
public class PlayPanel extends SmartTable
{
    public PlayPanel (final int gameId, int minPlayers, int maxPlayers)
    {
        super("playPanel", 0, 0);

        setText(0, 0, CGames.msgs.gdpPlay(), 1, "Title");

        int col = 0;
        if (minPlayers == 1 && maxPlayers != Integer.MAX_VALUE) {
            setWidget(1, col++, makePlayButton("SinglePlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", ""+gameId));
                }
            }));
        }
        if (maxPlayers > 1) {
            setWidget(1, col++, makePlayButton("FriendPlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "l", ""+gameId));
                }
            }));
        }
        getFlexCellFormatter().setColSpan(0, 0, col);
    }

    protected PushButton makePlayButton (String styleName, ClickListener onClick)
    {
        PushButton play = new PushButton("", onClick);
        play.setStyleName(styleName);
        play.addStyleName("Button");
        return play;
    }
}
