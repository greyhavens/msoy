//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Does something extraordinary.
 */
public class PlayPanel extends VerticalPanel
{
    public PlayPanel (int gameId, int minPlayers, int maxPlayers, int playersOnline)
    {
        setStyleName("playPanel");
        Widget play = new PlayButton(gameId, minPlayers, maxPlayers);
        add(play);
        setCellHorizontalAlignment(play, HasAlignment.ALIGN_CENTER);

        if (playersOnline > 0) {
            Widget online = MsoyUI.createLabel(CGames.msgs.featuredOnline(String.valueOf(playersOnline)), "Online");
            add(online);
            setCellHorizontalAlignment(online, HasAlignment.ALIGN_CENTER);
        }
    }
}
