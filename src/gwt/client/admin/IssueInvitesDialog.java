//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;

/**
 * Display a dialog for admins to issue invitations to the player base.
 */
public class IssueInvitesDialog extends BorderedDialog
{
    public IssueInvitesDialog ()
    {
        _header.add(createTitleLabel(CAdmin.msgs.invitesTitle(), null));

        FlexTable contents = (FlexTable)_contents;
        FlexCellFormatter formatter = contents.getFlexCellFormatter();
        contents.setCellSpacing(10);
        contents.setStyleName("sendInvites");


        _footer.add(new Button(CAdmin.cmsgs.dismiss(), new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        }));
        _footer.add(new Button(CAdmin.msgs.invitesIssueButton(), new ClickListener() {
            public void onClick (Widget widget) {
                // TODO: issue invtes
            }
        }));
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }
}
