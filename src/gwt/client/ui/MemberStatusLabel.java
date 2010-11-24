//
// $Id$

package client.ui;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.ShellMessages;
import client.util.Link;

/**
 * Displays a member's online (or last logon) status.
 */
public class MemberStatusLabel extends FlowPanel
{
    public MemberStatusLabel (MemberCard card)
    {
        setStyleName("memberStatus");

        if (card.status instanceof MemberCard.NotOnline) {
            long lastLogon = ((MemberCard.NotOnline)card.status).lastLogon;
            add(new InlineLabel(
                    _cmsgs.mslLastOnline(DateUtil.formatDateTime(new Date(lastLogon)))));

        } else if (card.status instanceof MemberCard.InGame) {
            MemberCard.InGame gs = (MemberCard.InGame)card.status;
            add(new InlineLabel(_cmsgs.mslOnlinePlaying(gs.gameName), true, false, true));

            Args args = (card.status instanceof MemberCard.InAVRGame) ?
                Args.compose("s" + ((MemberCard.InAVRGame)gs).sceneId) :
                Args.compose("game", "j", gs.gameId, card.name.getId());
            add(Link.create(_cmsgs.mslJoin(), Pages.WORLD, args));

        } else if (card.status instanceof MemberCard.InScene) {
            MemberCard.InScene ss = (MemberCard.InScene)card.status;
            add(new InlineLabel(_cmsgs.mslOnlineIn(ss.sceneName), true, false, true));
            add(Link.create(_cmsgs.mslJoin(), Pages.WORLD, "s" + ss.sceneId));
        }
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
