//
// $Id$

package client.profile;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.web.client.ProfileService;

import client.msgs.GroupInvite;
import client.msgs.MailComposition;
import client.shell.Application;
import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays a list of the groups of which a person is a member.
 */
public class GroupsBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.groups != null);
    }

    // @Override // from Blurb
    protected Panel createContent ()
    {
        return (_content = new FlexTable());
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.groupsTitle());

        if (pdata.groups.size() == 0) {
            setStatus(CProfile.getMemberId() == _name.getMemberId() ?
                      CProfile.msgs.notInGroupsSelf() : CProfile.msgs.notInGroupsOther());

        } else {
            for (int ii = 0, ll = pdata.groups.size(); ii < ll; ii++) {
                GroupMembership group = (GroupMembership)pdata.groups.get(ii);
                _content.setWidget(ii, 0, Application.groupViewLink(
                                       group.group.toString(), group.group.getGroupId()));
            }
        }

        if (CProfile.getMemberId() > 0 && CProfile.getMemberId() != _name.getMemberId()) {
            Button inviteButton = new Button(CProfile.msgs.inviteToGroup());
            new ClickCallback(inviteButton) {
                public boolean callService () {
                    CProfile.groupsvc.getMembershipGroups(
                        CProfile.ident, CProfile.getMemberId(), true, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    List inviteGroups = (List) result;
                    if (inviteGroups.size() == 0) {
                        MsoyUI.infoNear(CProfile.msgs.haveNoGroups(), _trigger);
                    } else {
                        new MailComposition(_name, "Join this group!",
                                            new GroupInvite.Composer(inviteGroups),
                                            "Check out this scrumptious group.").show();
                    }
                    return true;
                }
            };
            _content.setWidget(_content.getRowCount(), 0, inviteButton);
        }
    }

    protected void setStatus (String text)
    {
        _content.setText(0, 0, text);
    }

    protected SimplePanel _invitationPanel;
    protected FlexTable _content;
}
