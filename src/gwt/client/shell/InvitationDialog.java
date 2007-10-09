//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedDialog;
import client.util.MsoyUI;

public class InvitationDialog extends BorderedDialog 
{
    public static void display (final StatusPanel status, String inviteId)
    {
        CShell.membersvc.getInvitation(inviteId, true, new AsyncCallback () {
            public void onSuccess (Object result) {
                (new InvitationDialog(status, (Invitation)result)).show();
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CShell.serverError(cause));
            }
        });
    }

    // @Override // from BorderedDialog
    public Widget createContents ()
    {
        return new FlexTable();
    }

    protected InvitationDialog (final StatusPanel status, final Invitation invite) 
    {
        _header.add(createTitleLabel(CShell.cmsgs.inviteTitle(), null));

        FlexTable contents = (FlexTable)_contents;
        contents.setCellSpacing(10);
        contents.setStyleName("invitation");
        FlexCellFormatter formatter = contents.getFlexCellFormatter();

        int row = 0;
        formatter.setStyleName(row, 0, "Header");
        contents.setText(row++, 0, CShell.cmsgs.inviteIntro(invite.inviteeEmail));

        formatter.setColSpan(row, 0, 2);
        formatter.setStyleName(row, 0, "Body");
        contents.setText(row++, 0, CShell.cmsgs.inviteBody1(invite.inviter.toString()));

        formatter.setColSpan(row, 0, 2);
        formatter.setStyleName(row, 0, "Body");
        contents.setText(row++, 0, CShell.cmsgs.inviteBody2());

        _footer.add(new Button(CShell.cmsgs.inviteAccept(), new ClickListener() {
            public void onClick (Widget widget) {
                InvitationDialog.this.hide();
                (new CreateAccountDialog(status, invite)).show();
            }
        }));

        _footer.add(new Button(CShell.cmsgs.inviteReject(), new ClickListener() {
            public void onClick (Widget widget) {
                InvitationDialog.this.hide();
            }   
        }));
    }
}
