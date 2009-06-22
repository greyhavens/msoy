//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.TextBoxUtil;

/**
 * Page shown to the user after they request to delete their account and follow a confirmation
 * link.
 */
public class ConfirmDeletePanel extends FlowPanel
{
    public ConfirmDeletePanel (final String code)
    {
        setStyleName("editAccount");

        SmartTable table = new SmartTable(0, 10);
        table.setText(0, 0, _msgs.confirmDeleteTip(), 3, "Info");

        table.setWidget(1, 0, _confirm = new CheckBox(), 1, "rightLabel");
        table.setText(1, 1, _msgs.confirmDeleteConfirm(), 2, null);

        table.setText(2, 0, _msgs.confirmDeletePassword(), 1, "rightLabel");
        table.setWidget(2, 1, _password = new PasswordTextBox());
        table.getFlexCellFormatter().setWidth(2, 1, "10px");
        final Button delgo = new Button(_msgs.confirmDeleteDelete());
        table.setWidget(2, 2, delgo);
        TextBoxUtil.addTypingListener(_password, new Command() {
            public void execute () {
                delgo.setEnabled(_password.getText().trim().length() > 0);
            }
        });
        new ClickCallback<Void>(delgo) {
            protected boolean callService () {
                if (!_confirm.getValue()) {
                    MsoyUI.errorNear(_msgs.confirmDeleteMustCheck(), delgo);
                    return false;
                }
                _usersvc.deleteAccount(CShell.frame.md5hex(
                    _password.getText().trim()), code, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                Link.go(Pages.ACCOUNT, "deleted");
                return false;
            }
        };
        delgo.setEnabled(false);
        add(table);
    }

    protected CheckBox _confirm;
    protected PasswordTextBox _password;

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
