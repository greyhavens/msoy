//
// $Id$

package client.util;

import java.util.Date;

import com.google.gwt.user.client.ui.FlowPanel;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;

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
            add(new InlineLabel(CShell.cmsgs.mslLastOnline(_lfmt.format(new Date(lastLogon)))));

        } else if (status instanceof MemberCard.InGame) {
            MemberCard.InGame gs = (MemberCard.InGame)status;
            add(new InlineLabel(CShell.cmsgs.mslOnlinePlaying(gs.gameName), false, false, true));
            add(Link.create(CShell.cmsgs.mslJoin(), Page.WORLD,
                                       Args.compose("game", gs.gameId)));

        } else if (status instanceof MemberCard.InScene) {
            MemberCard.InScene ss = (MemberCard.InScene)status;
            add(new InlineLabel(CShell.cmsgs.mslOnlineIn(ss.sceneName), false, false, true));
            add(Link.create(CShell.cmsgs.mslJoin(), Page.WORLD, "s" + ss.sceneId));
        }
    }

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd h:mmaa");
}
