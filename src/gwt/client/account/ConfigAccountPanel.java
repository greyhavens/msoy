//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import com.threerings.gwt.ui.SmartTable;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NaviUtil;
import client.util.ServiceUtil;
import client.util.TextBoxUtil;

/**
 * Displays a streamlined "set your email address and password" UI. This is displayed to players
 * that created a Whirled account through an external authentication mechanism and who have done
 * something that requires that they configure a username and password.
 */
public class ConfigAccountPanel extends FlowPanel
{
    public ConfigAccountPanel ()
    {
        if (CShell.isGuest() || CShell.isPermaguest() ||
            !MemberMailUtil.isPlaceholderAddress(CShell.creds.accountName)) {
            SmartTable box = new SmartTable(0, 10);
            box.setText(0, 0, _msgs.configNotYou());
            box.setWidget(0, 1, Link.create(_msgs.configHere(), Pages.ACCOUNT, "edit"));
            add(box);
            return;
        }

        int row = 0;
        SmartTable table = new SmartTable(0, 10);
        table.setText(row++, 0, _msgs.configIntro(), 3, null);
        table.setHTML(row++, 0, "&nbsp;", 3, null);

        table.setText(row, 0, _msgs.editEmail(), 1, "rightLabel");
        _email = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
        table.setWidget(row++, 1, _email);
        TextBoxUtil.addTypingListener(_email, _validator);

        table.setText(row, 0, _msgs.configPassword(), 1, "rightLabel");
        table.setWidget(row++, 1, _password = new PasswordTextBox());
        TextBoxUtil.addTypingListener(_password, _validator);

        table.setText(row, 0, _msgs.editConfirm(), 1, "rightLabel");
        table.setWidget(row++, 1, _confirm = new PasswordTextBox());
        TextBoxUtil.addTypingListener(_confirm, _validator);

        table.setHTML(row++, 0, "&nbsp;", 3, null);
        table.setText(row++, 0, _msgs.configAuthTip(), 3, null);

        table.setWidget(row++, 2, _submit = MsoyUI.createButton(
            MsoyUI.MEDIUM_THIN, _msgs.configSubmit(), new ClickListener() {
            public void onClick (Widget widget) {
                updateAccount();
            }
        }));
        _submit.setEnabled(false);

        // we use a blank tongue box for formatting consistency with the edit page
        add(new TongueBox(null, table));
    }

    protected void updateAccount ()
    {
        // make sure the passwords match
        final String password = _password.getText().trim();
        if (!password.equals(_confirm.getText().trim())) {
            MsoyUI.errorNear(_msgs.editPasswordMismatch(), _confirm);
            return;
        }

        // we just let the email address get validated on the server (so lazy!)
        final String email = _email.getText().trim();

        // we could make a special service just for this, but this is fine for this rare page
        _usersvc.updateEmail(email, new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                CShell.frame.emailUpdated(email, false);
                _usersvc.updatePassword(CShell.frame.md5hex(password), new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        displayThanks();
                    }
                });
            }
        });
    }

    protected void displayThanks ()
    {
        clear();
        SmartTable box = new SmartTable(0, 10);
        box.setText(0, 0, _msgs.configThanks());
        box.setWidget(1, 0, MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.configGoBack(),
                                                NaviUtil.onGoBack()));
        box.getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
        add(box);
    }

    protected Command _validator = new Command() {
        public void execute () {
            _submit.setEnabled(_email.getText().trim().length() > 0 &&
                               _password.getText().trim().length() > 0 &&
                               _confirm.getText().trim().length() > 0);
        }
    };

    protected TextBox _email;
    protected PasswordTextBox _password;
    protected PasswordTextBox _confirm;
    protected PushButton _submit;

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
