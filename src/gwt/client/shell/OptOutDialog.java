//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Invitation;

import client.util.BorderedDialog;
import client.util.MsoyUI;

public class OptOutDialog extends BorderedDialog
{
    public static void display (String inviteId)
    {
        CShell.membersvc.getInvitation(inviteId, false, new AsyncCallback () {
            public void onSuccess (Object result) {
                (new OptOutDialog((Invitation)result)).show();
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

    protected OptOutDialog (final Invitation invite)
    {
        _header.add(createTitleLabel(CShell.cmsgs.optOutTitle(), null));

        FlexTable contents = (FlexTable)_contents;
        contents.setCellSpacing(10);
        // mimic the styles on InvitationDialog, as they are the same type of dialog
        contents.setStyleName("invitation");
        FlexCellFormatter formatter = contents.getFlexCellFormatter();

        int row = 0;
        formatter.setStyleName(row, 0, "Header");
        contents.setText(row++, 0, CShell.cmsgs.optOutIntro(invite.inviteeEmail));

        formatter.setColSpan(row, 0, 2);
        formatter.setStyleName(row, 0, "Body");
        contents.setText(row++, 0, CShell.cmsgs.optOutBody1());

        _footer.add(new Button(CShell.cmsgs.optOutAccept(), new ClickListener() {
            public void onClick (Widget widget) {
                OptOutDialog.this.hide();
                CShell.membersvc.optOut(invite, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        MsoyUI.info(CShell.cmsgs.optOutSuccessful(invite.inviteeEmail));
                    }
                    public void onFailure (Throwable cause) {
                        MsoyUI.error(CShell.serverError(cause));
                    }
                });
            }
        }));

        _footer.add(new Button(CShell.cmsgs.optOutReject(), new ClickListener() {
            public void onClick (Widget widget) {
                OptOutDialog.this.hide();
            }
        }));
    }
}
