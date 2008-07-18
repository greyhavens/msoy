//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Does something extraordinary.
 */
public class PlayPanel extends SmartTable
{
    public PlayPanel (final int gameId, int minPlayers, int maxPlayers, int playersOnline)
    {
        super("playPanel", 0, 0);

        Widget play;

        // Is this game single player only?
        if (minPlayers == 1 && maxPlayers == 1) {
            play = makePlayButton("SinglePlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", String.valueOf(gameId)));
                }
            });
        } else {
            play = makePlayButton("FriendPlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "l", ""+gameId));
                }
            });
        }
        int row = addWidget(play, 1, null);
        //getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);

        if (playersOnline > 0) {
            addWidget(WidgetUtil.makeShim(10, 10), 3, null);
            addWidget(MsoyUI.createLabel(CGames.msgs.featuredOnline(""+playersOnline), "Online"),
                      3, null);
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        }
    }

    protected PushButton makePlayButton (String styleName, ClickListener onClick)
    {
        PushButton play = new PushButton("", onClick);
        play.setStyleName(styleName);
        play.addStyleName("Button");
        return play;
    }
}
