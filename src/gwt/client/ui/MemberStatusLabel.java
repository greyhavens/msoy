//
// $Id$

package client.ui;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.shell.ShellMessages;
import client.shell.Pages;
import client.util.Link;

/**
 * Displays a member's online (or last logon) status.
 */
public class MemberStatusLabel extends FlowPanel
{
    public MemberStatusLabel (MemberCard.Status status)
    {
        setStyleName("memberStatus");

        if (status instanceof MemberCard.NotOnline) {
            long lastLogon = ((MemberCard.NotOnline)status).lastLogon;
            add(new InlineLabel(_cmsgs.mslLastOnline(MsoyUI.formatDateTime(new Date(lastLogon)))));

        } else if (status instanceof MemberCard.InGame) {
            MemberCard.InGame gs = (MemberCard.InGame)status;
            add(new InlineLabel(_cmsgs.mslOnlinePlaying(gs.gameName), true, false, true));
            add(Link.create(_cmsgs.mslJoin(), Pages.WORLD,
                                       Args.compose("game", gs.gameId)));

        } else if (status instanceof MemberCard.InScene) {
            MemberCard.InScene ss = (MemberCard.InScene)status;
            add(new InlineLabel(_cmsgs.mslOnlineIn(ss.sceneName), true, false, true));
            add(Link.create(_cmsgs.mslJoin(), Pages.WORLD, "s" + ss.sceneId));
        }
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
