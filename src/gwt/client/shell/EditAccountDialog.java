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

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.AccountInfo;

import client.util.BorderedDialog;

/**
 * Displays account information, allows twiddling.
 */
public class EditAccountDialog extends BorderedDialog
{
    public EditAccountDialog (AccountInfo accountInfo)
    {
        _header.add(createTitleLabel(CShell.cmsgs.editTitle(), null));

        _accountInfo = accountInfo;
        FlexTable contents = (FlexTable)_contents;
        contents.setCellSpacing(10);
        contents.setStyleName("editAccount");

        int row = 0;

        if (CShell.creds.permaName == null) {
            contents.getFlexCellFormatter().setStyleName(row, 0, "Header");
            contents.getFlexCellFormatter().setColSpan(row, 0, 3);
            contents.setText(row++, 0, CShell.cmsgs.editPickPermaNameHeader());

            contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            contents.setText(row, 0, CShell.cmsgs.editPermaName());
            contents.setWidget(row, 1, _pname = new TextBox());
            _pname.addKeyboardListener(_valpname);
            _uppname = new Button(CShell.cmsgs.submit(), new ClickListener() {
                public void onClick (Widget widget) {
                    configurePermaName();
                }
            });
            _uppname.setEnabled(false);
            contents.setWidget(_permaRow = row++, 2, _uppname);

            contents.getFlexCellFormatter().setStyleName(row, 0, "Tip");
            contents.getFlexCellFormatter().setColSpan(row, 0, 3);
            contents.setHTML(row++, 0, CShell.cmsgs.editPermaNameTip());

        } else {
            contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            contents.setText(row, 0, CShell.cmsgs.editPermaName());
            contents.getFlexCellFormatter().setStyleName(row, 1, "PermaName");
            contents.setText(row++, 1, CShell.creds.permaName);
        }

        contents.getFlexCellFormatter().setStyleName(row, 0, "Header");
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.editRealNameHeader());
        
        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.editRealName());
        contents.setWidget(row, 1, _rname = new TextBox());
        _rname.setText(_accountInfo.realName);
        _rname.addKeyboardListener(_valrname);
        _uprname = new Button(CShell.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateRealName();
            }
        });
        _uprname.setEnabled(false);
        contents.setWidget(row++, 2, _uprname);

        contents.getFlexCellFormatter().setStyleName(row, 0, "Tip");
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.setHTML(row++, 0, CShell.cmsgs.editRealNameTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "Header");
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.editEmailHeader());

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

        contents.getFlexCellFormatter().setStyleName(row, 0, "Header");
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.setText(row++, 0, CShell.cmsgs.editPasswordHeader());

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
        contents.setWidget(row++, 0, _status = new Label(CShell.cmsgs.editTip()));

        _footer.add(new Button(CShell.cmsgs.dismiss(), new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        }));
    }

    protected void updateRealName ()
    {
        final String oldRealName = _accountInfo.realName;
        _accountInfo.realName = _rname.getText().trim();
        _uprname.setEnabled(false);
        _rname.setEnabled(false);
        CShell.usersvc.updateAccountInfo(CShell.ident, _accountInfo, new AsyncCallback() {
            public void onSuccess (Object result) {
                _rname.setEnabled(true);
                _uprname.setEnabled(false);
                setStatus(CShell.cmsgs.realNameUpdated());
            }
            public void onFailure (Throwable cause) {
                _rname.setText(_accountInfo.realName = oldRealName);
                _rname.setEnabled(true);
                _uprname.setEnabled(true);
                setError(CShell.serverError(cause));
            }
        });
    }

    protected void updateEmail ()
    {
        final String email = _email.getText().trim();
        _upemail.setEnabled(false);
        _email.setEnabled(false);
        CShell.usersvc.updateEmail(CShell.ident, email, new AsyncCallback() {
            public void onSuccess (Object result) {
                _email.setEnabled(true);
                CShell.creds.accountName = email;
                setStatus(CShell.cmsgs.emailUpdated());
            }
            public void onFailure (Throwable cause) {
                _email.setEnabled(true);
                _upemail.setEnabled(true);
                setError(CShell.serverError(cause));
            }
        });
    }

    protected void updatePassword ()
    {
        final String password = CShell.md5hex(_password.getText().trim());
        _uppass.setEnabled(false);
        _password.setEnabled(false);
        _confirm.setEnabled(false);
        CShell.usersvc.updatePassword(CShell.ident, password, new AsyncCallback() {
            public void onSuccess (Object result) {
                _password.setText("");
                _password.setEnabled(true);
                _confirm.setText("");
                _confirm.setEnabled(true);
                setStatus(CShell.cmsgs.passwordUpdated());
            }
            public void onFailure (Throwable cause) {
                _password.setEnabled(true);
                _confirm.setEnabled(true);
                _uppass.setEnabled(true);
                setError(CShell.serverError(cause));
            }
        });
    }

    protected void configurePermaName ()
    {
        final String pname = _pname.getText().trim();
        _uppname.setEnabled(false);
        _pname.setEnabled(false);
        CShell.usersvc.configurePermaName(CShell.ident, pname, new AsyncCallback() {
            public void onSuccess (Object result) {
                CShell.creds.permaName = pname;
                FlexTable contents = (FlexTable)_contents;
                contents.getFlexCellFormatter().setStyleName(_permaRow, 1, "PermaName");
                contents.setText(_permaRow, 1, pname);
                contents.setText(_permaRow, 2, "");
                contents.setText(_permaRow+1, 0, "");
                setStatus(CShell.cmsgs.permaNameConfigured());
            }
            public void onFailure (Throwable cause) {
                _pname.setEnabled(true);
                _uppname.setEnabled(true);
                setError(CShell.serverError(cause));
            }
        });
    }

    protected void validateRealName ()
    {
        String realName = _rname.getText().trim();
        boolean valid = false;
        if (!_accountInfo.realName.equals(realName)) {
            setStatus(CShell.cmsgs.editNameReady());
            valid = true;
        } else {
            setStatus("");
        }
        _uprname.setEnabled(valid);
    }

    protected void validateEmail ()
    {
        String email = _email.getText().trim();
        boolean valid = false;
        if (email.length() < 4 || email.indexOf("@") == -1 ||
            email.equals(CShell.creds.accountName)) {
            setStatus("");
        } else {
            setStatus(CShell.cmsgs.editEmailReady());
            valid = true;
        }
        _upemail.setEnabled(valid);
    }

    protected void validatePasswords ()
    {
        boolean valid = false;
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (confirm.length() == 0) {
            setError(CShell.cmsgs.editMissingConfirm());
        } else if (!password.equals(confirm)) {
            setError(CShell.cmsgs.editPasswordMismatch());
        } else {
            setStatus(CShell.cmsgs.editPasswordReady());
            valid = true;
        }
        _uppass.setEnabled(valid);
    }

    protected void validatePermaName ()
    {
        String pname = _pname.getText().trim();
        for (int ii = 0; ii < pname.length(); ii++) {
            char c = pname.charAt(ii);
            if ((ii == 0 && !Character.isLetter(c)) ||
                (!Character.isLetter(c) && !Character.isDigit(c) && c != '_')) {
                setError(CShell.cmsgs.editPermaInvalid());
                _uppname.setEnabled(false);
                return;
            }
        }

        boolean valid = false;
        if (pname.length() == 0) {
            setStatus("");
        } else if (pname.length() < MemberName.MINIMUM_PERMANAME_LENGTH) {
            setError(CShell.cmsgs.editPermaShort());
        } else if (pname.length() > MemberName.MAXIMUM_PERMANAME_LENGTH) {
            setError(CShell.cmsgs.editPermaLong());
        } else {
            setStatus(CShell.cmsgs.editPermaReady());
            valid = true;
        }
        _uppname.setEnabled(valid);
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }

    protected void setError (String text) 
    {
        _status.addStyleName("Error");
        _status.setText(text);
    }

    protected void setStatus (String text) 
    {
        _status.removeStyleName("Error");
        _status.setText(text);
    }

    protected KeyboardListener _valrname = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validateRealName();
                }
            });
        }
    };

    protected KeyboardListener _valemail = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validateEmail();
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

    protected KeyboardListener _valpname = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validatePermaName();
                }
            });
        }
    };

    protected TextBox _email, _pname, _rname;
    protected PasswordTextBox _password, _confirm;
    protected Button _upemail, _uppass, _uppname, _uprname;
    protected int _permaRow;
    protected Label _status;
    protected AccountInfo _accountInfo;
}
