package client.games;

import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;

/**
 * Displays a single play button.  If multiplayer exists, show a Play with friends button,
 * otherwise show a Play just me button.
 */
public class PlayButton extends SimplePanel
{
    public PlayButton (final int gameId, int minPlayers, int maxPlayers)
    {
        setStyleName("playButton");
        if (maxPlayers == 1) {
            PushButton singleButton = makePlayButton("SinglePlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", "" + gameId));
                }
            });
            add(singleButton);
        }
        else {
            PushButton multiButton = makePlayButton("FriendPlay", new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "l", "" + gameId));
                }
            });
            add(multiButton);
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
