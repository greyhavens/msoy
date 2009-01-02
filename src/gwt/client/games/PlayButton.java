//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.NoopAsyncCallback;
import client.util.ServiceUtil;

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

        // this is only used for testing game loading issues per WRLD-531, and will
        // be removed after the test is over. -- robert
        if (! inWorld) {
            play.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _membersvc.trackClientAction(CShell.visitor, "WRLD-531 game started", "stage 1",
                        new NoopAsyncCallback() { });
                }
            });
        }

        play.addClickListener(Link.createListener(Pages.WORLD, args));
        return play;
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
