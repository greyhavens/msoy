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
    public PlayPanel (final int gameId, int minPlayers, int maxPlayers)
    {
        super("playPanel", 0, 0);

        setText(0, 0, CGames.msgs.gdpPlay(), 3, "Title");

        Widget single;
        if (minPlayers == 1) {
            single = makePlayButton("SinglePlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", ""+gameId));
                }
            });
        } else {
            single = MsoyUI.createLabel(CGames.msgs.gdpNoSingle(), "NoSingle");
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
        getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_MIDDLE);
        setWidget(1, 1, WidgetUtil.makeShim(15, 15));
        setWidget(1, 2, multi);
        getFlexCellFormatter().setVerticalAlignment(1, 2, HasAlignment.ALIGN_MIDDLE);
    }

    protected PushButton makePlayButton (String styleName, ClickListener onClick)
    {
        PushButton play = new PushButton("", onClick);
        play.setStyleName(styleName);
        play.addStyleName("Button");
        return play;
    }
}
