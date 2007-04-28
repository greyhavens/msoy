//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;
import client.util.NumberTextBox;

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

        int row = 0;

        formatter.setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CAdmin.msgs.invitesNumber());
        contents.setWidget(row++, 1, _numberInvites = new NumberTextBox(false));
        _numberInvites.setMaxLength(3);
        _numberInvites.setVisibleLength(3);

        formatter.setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CAdmin.msgs.invitesIssueSelection());
        contents.setWidget(row++, 1, _issueToSelection = new ListBox());
        _issueToSelection.addItem(CAdmin.msgs.invitesToAll());
        _issueToSelection.addItem(CAdmin.msgs.invitesToActive());

        formatter.setStyleName(row, 0, "Tip");
        formatter.setColSpan(row, 0, 2);
        contents.setText(row, 0, CAdmin.msgs.activeUsersTip());

        _footer.add(new Button(CAdmin.cmsgs.cancel(), new ClickListener() {
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

    protected NumberTextBox _numberInvites;
    protected ListBox _issueToSelection;
}
