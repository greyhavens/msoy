//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a play button that does the right thing in for all of the myriad modalities a game
 * might represent.
 */
public class PlayButton
{
    public enum Size { SMALL, MEDIUM, LARGE };

    public static Widget create (GameInfo info, String noGroupMessage, Size size)
    {
        return create(info.gameId, info.minPlayers, info.maxPlayers,
                      info.isInWorld, info.groupId, noGroupMessage, size);
    }

    public static Widget create (int gameId, int minPlayers, int maxPlayers,
                                 boolean inWorld, int groupId, String noGroupMessage, Size size)
    {
        String args;
        if (inWorld) {
            if (groupId == 0) {
                return MsoyUI.createLabel(noGroupMessage, null);
            }
            args = "g" + groupId;
        } else if (minPlayers == 1 && maxPlayers == 1) {
            args = Args.compose("game", "s", "" + gameId);
        } else {
            args = Args.compose("game", "l", "" + gameId);
        }

        PushButton play;
        switch (size) {
        default:
        case SMALL:
            play = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.playPlay(), null);
            break;
        case MEDIUM:
            play = new PushButton();
            play.setStyleName("playButtonMedium");
            break;
        case LARGE:
            play = new PushButton();
            play.setStyleName("playButtonLarge");
            break;
        }
        play.addClickListener(Link.createListener(Pages.WORLD, args));
        return play;
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
