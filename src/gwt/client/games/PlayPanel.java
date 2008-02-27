//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

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

        setText(0, 0, CGames.msgs.gdpPlay(), 3, "Title");

        Widget single = WidgetUtil.makeShim(120, 44);
        if (minPlayers == 1 && maxPlayers != Integer.MAX_VALUE) {
            single = makePlayButton("SinglePlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", ""+gameId));
                }
            });
        }
        Widget multi = WidgetUtil.makeShim(120, 44);
        if (maxPlayers > 1) {
            multi = makePlayButton("FriendPlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "l", ""+gameId));
                }
            });
        }
        setWidget(1, 0, single);
        setWidget(1, 1, WidgetUtil.makeShim(15, 15));
        setWidget(1, 2, multi);
    }

    protected PushButton makePlayButton (String styleName, ClickListener onClick)
    {
        PushButton play = new PushButton("", onClick);
        play.setStyleName(styleName);
        play.addStyleName("Button");
        return play;
    }
}
