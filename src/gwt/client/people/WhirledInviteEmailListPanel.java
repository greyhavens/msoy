//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.InviteServiceAsync;
import com.threerings.msoy.web.gwt.EmailContact;

import client.ui.MsoyUI;
import client.util.InfoCallback;

/**
 * An email list panel customized for sending Whirled invitations.
 */
public class WhirledInviteEmailListPanel extends EmailListPanel
{
    public WhirledInviteEmailListPanel ()
    {
        super(true);

        // if we just finished phase two of our registration process then we may have slurped email
        // addresses sitting around in this static field for us to handily grab and display
        if (FindFriendsPanel.contacts.size() > 0) {
            for (EmailContact ec : FindFriendsPanel.contacts.values()) {
                addAddress(ec.name, ec.email);
            }
        }
    }

    @Override // from EmailListPanel
    protected void addFrom (SmartTable table)
    {
        super.addFrom(table);

        int row = table.addText(_msgs.inviteSubject(), 1, "Bold");
        table.getFlexCellFormatter().setWidth(row, 0, "10px"); // squeezy!
        _subject = MsoyUI.createTextBox("", InviteUtils.MAX_SUBJECT_LENGTH, 25);
        table.setWidget(row, 1, _subject);
    }

    @Override // from EmailListPanel
    protected void handleSend (String from, String msg, final List<EmailContact> addrs)
    {
        if (addrs.isEmpty()) {
            MsoyUI.info(_msgs.inviteEnterAddresses());
            return;
        }

        String subject = _subject.getText().trim();
        if (subject.length() < InviteUtils.MIN_SUBJECT_LENGTH) {
            MsoyUI.errorNear(_msgs.inviteSubjectTooShort(""+InviteUtils.MIN_SUBJECT_LENGTH),
                             _subject);
            _subject.setFocus(true);
            return;
        }

        // sendEvent("invited", new NoopAsyncCallback());

        _invitesvc.sendInvites(
            addrs, from, subject, msg, false, new InfoCallback<InvitationResults>() {
                public void onSuccess (InvitationResults ir) {
                    _addressList.clear();
                    InviteUtils.showInviteResults(addrs, ir);
                }
            });
    }

    @Override // from EmailListPanel
    protected void handleExistingMembers (List<EmailContact> addrs)
    {
        InviteUtils.ResultsPopup rp = new InviteUtils.ResultsPopup(_msgs.webmailResults());
        int row = 0;
        SmartTable contents = rp.getContents();
        contents.setText(row++, 0, _msgs.inviteResultsMembers());
        for (EmailContact ec : addrs) {
            contents.setText(row, 0, _msgs.inviteMember(ec.name, ec.email));
            if (ec.friendship == Friendship.FRIENDS) {
                contents.setText(row++, 2, _msgs.mlAlreadyFriend());

            } else if (ec.friendship == Friendship.INVITED) {
                contents.setText(row++, 2, _msgs.mlAlreadyFriendInv());

            } else {
                ClickHandler onClick = new FriendInviter(ec.mname, "InvitePanel");
                contents.setWidget(
                    row, 1, MsoyUI.createActionImage("/images/profile/addfriend.png", onClick));
                contents.setWidget(
                    row++, 2, MsoyUI.createActionLabel(_msgs.mlAddFriend(), onClick));
            }
        }
        rp.show();
    }

    protected TextBox _subject;

    protected static final InviteServiceAsync _invitesvc = GWT.create(InviteService.class);
}
