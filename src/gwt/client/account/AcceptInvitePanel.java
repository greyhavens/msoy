//
// $Id$

package client.account;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Invitation;

import client.shell.Frame;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;

public class AcceptInvitePanel extends FlexTable
{
    public AcceptInvitePanel (index parent, String inviteId)
    {
        setCellSpacing(10);
        setStyleName("invitation");

        _parent = parent;

        Frame.setTitle(CAccount.msgs.welcomeTitle(), CAccount.msgs.inviteTitle());
        CAccount.membersvc.getInvitation(inviteId, true, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((Invitation)result);
            }
        });
    }

    protected void init (final Invitation invite) 
    {
        if (invite == null) {
            setText(0, 0, CAccount.msgs.inviteMissing());
            return;
        }

        int row = 0;
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        setText(row++, 0, CAccount.msgs.inviteIntro(invite.inviteeEmail));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        if (invite.inviter == null) {
            setText(row++, 0, CAccount.msgs.inviteBody1anon());
        } else {
            setText(row++, 0, CAccount.msgs.inviteBody1(invite.inviter.toString()));
        }

        getFlexCellFormatter().setColSpan(row, 0, 2);
        setText(row++, 0, CAccount.msgs.inviteBody2());

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(new Button(CAccount.msgs.inviteAccept(), new ClickListener() {
            public void onClick (Widget widget) {
                _parent.showCreateAccount(invite);
            }
        }));
        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, footer);
    }

    protected index _parent;
}
