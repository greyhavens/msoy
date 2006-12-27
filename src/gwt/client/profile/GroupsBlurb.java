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

import com.threerings.gwt.ui.Anchor;

import com.threerings.msoy.web.data.GroupMembership;

import client.msgs.GroupInvite;
import client.msgs.MailComposition;
import client.shell.MsoyEntryPoint;

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
        setHeader("Groups");

        List blurbGroups = (List)blurbData;
        if (blurbGroups.size() == 0) {
            setStatus("You're not a member of any groups. Boo hoo.");

        } else {
            for (int ii = 0, ll = blurbGroups.size(); ii < ll; ii++) {
                GroupMembership group = (GroupMembership)blurbGroups.get(ii);
                Anchor link = new Anchor(MsoyEntryPoint.groupViewPath(group.group.groupId),
                    group.group.groupName);
                _content.setWidget(ii, 0, link);
            }
        }
        _invitationPanel = new SimplePanel();
        _content.setWidget(_content.getRowCount(), 0, _invitationPanel);

        if (_ctx.creds.memberId != _memberId) {
            _ctx.groupsvc.getMembershipGroups(
                _ctx.creds, _ctx.creds.memberId, true, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        final List inviteGroups = (List) result;
                        if (inviteGroups.size() == 0) {
                            return;
                        }
                        Button inviteButton = new Button("Invite To A Group");
                        inviteButton.addClickListener(new ClickListener() {
                            public void onClick (Widget sender) {
                                new MailComposition(_ctx, _memberId, "Join this group!",
                                                    new GroupInvite.Composer(inviteGroups),
                                                    "Check out this scrumptious group.").show();
                            }
                        });
                        _invitationPanel.setWidget(inviteButton);
                    }
                    public void onFailure (Throwable caught) {
                        // TODO: silently ignore? need coherent error strategy.
                    }
                });
        }
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setHeader("Error");
        setStatus("Failed to load friends: " + cause);
    }

    protected void setStatus (String text)
    {
        _content.setText(0, 0, text);
    }
    protected SimplePanel _invitationPanel;
    protected FlexTable _content;
}
