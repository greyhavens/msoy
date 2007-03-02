//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.EnterClickAdapter;

import client.util.BorderedDialog;

/**
 * Displays account information, allows twiddling.
 */
public class EditAccountDialog extends BorderedDialog
{
    public EditAccountDialog ()
    {
        _header.add(createTitleLabel(CShell.cmsgs.editTitle(), null));

        FlexTable contents = (FlexTable)_contents;
        contents.setCellSpacing(10);
        contents.setStyleName("editAccount");

        int row = 0;
        contents.getFlexCellFormatter().setStyleName(row, 0, "Intro");
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.editTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.editEmail());
        contents.setWidget(row, 1, _email = new TextBox());
        _email.setText(CShell.creds.accountName);
        _email.addKeyboardListener(_valemail);
        _upemail = new Button(CShell.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmail();
            }
        });
        _upemail.setEnabled(false);
        contents.setWidget(row++, 2, _upemail);

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.editPassword());
        contents.setWidget(row++, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_valpass);

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.editConfirm());
        contents.setWidget(row, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(_valpass);
        _uppass = new Button(CShell.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updatePassword();
            }
        });
        contents.setWidget(row++, 2, _uppass);
        _uppass.setEnabled(false);

        contents.getFlexCellFormatter().setStyleName(row, 0, "Status");
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.setWidget(row++, 0, _status = new Label(""));

        _footer.add(new Button(CShell.cmsgs.dismiss(), new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        }));
    }

    protected void updateEmail ()
    {
        final String email = _email.getText();
        _upemail.setEnabled(false);
        _email.setEnabled(false);
        CShell.usersvc.updateEmail(CShell.creds, email, new AsyncCallback() {
            public void onSuccess (Object result) {
                _email.setEnabled(true);
                CShell.creds.accountName = email;
                _status.setText(CShell.cmsgs.emailUpdated());
            }
            public void onFailure (Throwable cause) {
                _email.setEnabled(true);
                _upemail.setEnabled(true);
                _status.setText(CShell.serverError(cause));
            }
        });
    }

    protected void updatePassword ()
    {
        final String password = md5hex(_password.getText());
        _uppass.setEnabled(false);
        _password.setEnabled(false);
        _confirm.setEnabled(false);
        CShell.usersvc.updatePassword(CShell.creds, password, new AsyncCallback() {
            public void onSuccess (Object result) {
                _password.setText("");
                _password.setEnabled(true);
                _confirm.setText("");
                _confirm.setEnabled(true);
                _status.setText(CShell.cmsgs.passwordUpdated());
            }
            public void onFailure (Throwable cause) {
                _password.setEnabled(true);
                _confirm.setEnabled(true);
                _uppass.setEnabled(true);
                _status.setText(CShell.serverError(cause));
            }
        });
    }

    protected void validatePasswords ()
    {
        boolean valid = false;
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (confirm.length() == 0) {
            _status.setText(CShell.cmsgs.editMissingConfirm());
        } else if (!password.equals(confirm)) {
            _status.setText(CShell.cmsgs.editPasswordMismatch());
        } else {
            _status.setText("");
            valid = true;
        }
        _uppass.setEnabled(valid);
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }

    protected native String md5hex (String text) /*-{
       return $wnd.hex_md5(text);
    }-*/;

    protected KeyboardListener _valemail = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    _upemail.setEnabled(!_email.getText().equals(CShell.creds.accountName));
                }
            });
        }
    };

    protected KeyboardListener _valpass = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validatePasswords();
                }
            });
        }
    };

    protected TextBox _email;
    protected PasswordTextBox _password, _confirm;
    protected Button _upemail, _uppass;
    protected Label _status;
}
