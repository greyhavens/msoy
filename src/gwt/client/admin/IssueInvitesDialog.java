//
// $Id$

package client.admin;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;

import client.shell.ShellMessages;
import client.util.BorderedDialog;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.NumberTextBox;
import client.util.ServiceUtil;

/**
 * Display a dialog for admins to issue invitations to the player base.
 */
public class IssueInvitesDialog extends BorderedDialog
{
    public IssueInvitesDialog ()
    {
        setHeaderTitle(CAdmin.msgs.invitesTitle());

        FlexTable contents = new FlexTable();
        FlexTable.FlexCellFormatter formatter = contents.getFlexCellFormatter();
        contents.setCellSpacing(10);
        contents.setStyleName("sendInvites");

        int row = 0;

        formatter.setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CAdmin.msgs.invitesNumber());
        contents.setWidget(row++, 1, _numberInvites = new NumberTextBox(false, 3));

        formatter.setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CAdmin.msgs.invitesIssueSelection());
        contents.setWidget(row++, 1, _issueToSelection = new ListBox());
        _issueToSelection.addItem(CAdmin.msgs.invitesToAll());
        _issueToSelection.addItem(CAdmin.msgs.invitesToActive());

        formatter.setStyleName(row, 0, "Tip");
        formatter.setColSpan(row, 0, 2);
        contents.setText(row, 0, CAdmin.msgs.activeUsersTip());
        setContents(contents);

        addButton(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        }));
        addButton(new Button(CAdmin.msgs.invitesIssueButton(), new ClickListener() {
            public void onClick (Widget widget) {
                Date activeSince = null;
                if (_issueToSelection.getSelectedIndex() == 1) {
                    // one week ago
                    activeSince = new Date((new Date()).getTime() - 7 * 24 * 60 * 60 * 1000);
                }
                _adminsvc.grantInvitations(CAdmin.ident, _numberInvites.getValue().intValue(),
                    activeSince, new MsoyCallback<Void>() {
                        public void onSuccess (Void result) {
                            IssueInvitesDialog.this.hide();
                            String msg = _issueToSelection.getSelectedIndex() == 0 ?
                                CAdmin.msgs.invitesToAll() : CAdmin.msgs.invitesToActive();
                            String count = _numberInvites.getValue().toString();
                            MsoyUI.info(CAdmin.msgs.invitesSuccess(msg, count));
                        }
                    });
            }
        }));
    }

    protected NumberTextBox _numberInvites;
    protected ListBox _issueToSelection;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
