//
// $Id$

package client.util;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.shell.ShellMessages;
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
            add(new InlineLabel(_cmsgs.mslLastOnline(_lfmt.format(new Date(lastLogon)))));

        } else if (status instanceof MemberCard.InGame) {
            MemberCard.InGame gs = (MemberCard.InGame)status;
            add(new InlineLabel(_cmsgs.mslOnlinePlaying(gs.gameName), false, false, true));
            add(Link.create(_cmsgs.mslJoin(), Page.WORLD,
                                       Args.compose("game", gs.gameId)));

        } else if (status instanceof MemberCard.InScene) {
            MemberCard.InScene ss = (MemberCard.InScene)status;
            add(new InlineLabel(_cmsgs.mslOnlineIn(ss.sceneName), false, false, true));
            add(Link.create(_cmsgs.mslJoin(), Page.WORLD, "s" + ss.sceneId));
        }
    }

    protected static final SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd h:mmaa");
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
