//
// $Id$

package client.profile;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.GroupMembership;

import client.msgs.GroupInvite;
import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.InfoPopup;

/**
 * Displays a list of the groups of which a person is a member.
 */
public class GroupsBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        return (_content = new FlexTable());
    }

    // @Override // from Blurb
    protected void didInit (Object blurbData)
    {
        setHeader(CProfile.msgs.groupsTitle());

        List blurbGroups = (List)blurbData;
        if (blurbGroups.size() == 0) {
            setStatus(CProfile.getMemberId() == _memberId ? CProfile.msgs.notInGroupsSelf() :
                      CProfile.msgs.notInGroupsOther());

        } else {
            for (int ii = 0, ll = blurbGroups.size(); ii < ll; ii++) {
                GroupMembership group = (GroupMembership)blurbGroups.get(ii);
                _content.setWidget(
                    ii, 0, Application.groupViewLink(group.group.groupName, group.group.groupId));
            }
        }

        if (CProfile.getMemberId() > 0 && CProfile.getMemberId() != _memberId) {
            Button inviteButton = new Button(CProfile.msgs.inviteToGroup());
            new ClickCallback(inviteButton) {
                public boolean callService () {
                    CProfile.groupsvc.getMembershipGroups(
                        CProfile.creds, CProfile.getMemberId(), true, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    List inviteGroups = (List) result;
                    if (inviteGroups.size() == 0) {
                        new InfoPopup(CProfile.msgs.haveNoGroups()).showNear(_trigger);
                    } else {
                        new MailComposition(_memberId, "Join this group!",
                                            new GroupInvite.Composer(inviteGroups),
                                            "Check out this scrumptious group.").show();
                    }
                    return true;
                }
            };
            _content.setWidget(_content.getRowCount(), 0, inviteButton);
        }
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setHeader(CProfile.msgs.errorTitle());
        setStatus("Failed to load friends: " + cause);
    }

    protected void setStatus (String text)
    {
        _content.setText(0, 0, text);
    }

    protected SimplePanel _invitationPanel;
    protected FlexTable _content;
}
