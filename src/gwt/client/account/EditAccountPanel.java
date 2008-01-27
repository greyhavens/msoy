//
// $Id$

package client.account;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
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

import client.shell.Frame;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays account information, allows twiddling.
 */
public class EditAccountPanel extends FlexTable
{
    public EditAccountPanel ()
    {
        setCellSpacing(10);
        setStyleName("editAccount");

        Frame.setTitle(CAccount.msgs.accountTitle(), CAccount.msgs.editSubtitle());

        CAccount.usersvc.getAccountInfo(CAccount.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((AccountInfo)result);
            }
        });
    }

    protected void init (AccountInfo accountInfo)
    {
        _accountInfo = accountInfo;

        int row = 0;

        // configure or display permaname interface
        if (CAccount.creds.permaName == null) {
            getFlexCellFormatter().setStyleName(row, 0, "Header");
            getFlexCellFormatter().setColSpan(row, 0, 3);
            setText(row++, 0, CAccount.msgs.editPickPermaNameHeader());

            getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            setText(row, 0, CAccount.msgs.editPermaName());
            setWidget(row, 1, _pname = new TextBox());
            _pname.addKeyboardListener(_valpname);
            _uppname = new Button(CAccount.cmsgs.submit(), new ClickListener() {
                public void onClick (Widget widget) {
                    configurePermaName();
                }
            });
            _uppname.setEnabled(false);
            setWidget(_permaRow = row++, 2, _uppname);

            getFlexCellFormatter().setStyleName(row, 0, "Tip");
            getFlexCellFormatter().setColSpan(row, 0, 3);
            setHTML(row++, 0, CAccount.msgs.editPermaNameTip());

        } else {
            getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            setText(row, 0, CAccount.msgs.editPermaName());
            getFlexCellFormatter().setStyleName(row, 1, "PermaName");
            setText(row++, 1, CAccount.creds.permaName);
        }

        // configure email address interface
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        getFlexCellFormatter().setColSpan(row, 0, 3);
        setText(row++, 0, CAccount.msgs.editEmailHeader());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.editEmail());
        setWidget(row, 1, _email = new TextBox());
        _email.setText(CAccount.creds.accountName);
        _email.addKeyboardListener(_valemail);
        _upemail = new Button(CAccount.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmail();
            }
        });
        _upemail.setEnabled(false);
        setWidget(row++, 2, _upemail);

        // configure email preferences interface
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        getFlexCellFormatter().setColSpan(row, 0, 3);
        setText(row++, 0, CAccount.msgs.editEPrefsHeader());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.editWhirledMailEmail());
        RowPanel bits = new RowPanel();
        bits.add(_whirledEmail = new CheckBox());
        bits.add(MsoyUI.createLabel(CAccount.msgs.editWhirledMailEmailTip(), "tipLabel"));
        getFlexCellFormatter().setColSpan(row, 1, 2);
        setWidget(row++, 1, bits);
        _whirledEmail.setChecked(_accountInfo.emailWhirledMail);

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.editAnnounceEmail());
        bits = new RowPanel();
        bits.add(_announceEmail = new CheckBox());
        bits.add(MsoyUI.createLabel(CAccount.msgs.editAnnounceEmailTip(), "tipLabel"));
        getFlexCellFormatter().setColSpan(row, 1, 2);
        setWidget(row++, 1, bits);
        _announceEmail.setChecked(_accountInfo.emailAnnouncements);

        _upeprefs = new Button(CAccount.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmailPrefs();
            }
        });
        setWidget(row++, 2, _upeprefs);

        // configure real name interface
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        getFlexCellFormatter().setColSpan(row, 0, 3);
        setText(row++, 0, CAccount.msgs.editRealNameHeader());
        
        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.editRealName());
        setWidget(row, 1, _rname = new TextBox());
        _rname.setText(_accountInfo.realName);
        _rname.addKeyboardListener(_valrname);
        _uprname = new Button(CAccount.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateRealName();
            }
        });
        _uprname.setEnabled(false);
        setWidget(row++, 2, _uprname);

        getFlexCellFormatter().setStyleName(row, 0, "Tip");
        getFlexCellFormatter().setColSpan(row, 0, 3);
        setHTML(row++, 0, CAccount.msgs.editRealNameTip());

        // configure password interface
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        getFlexCellFormatter().setColSpan(row, 0, 3);
        setText(row++, 0, CAccount.msgs.editPasswordHeader());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.editPassword());
        setWidget(row++, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_valpass);

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.editConfirm());
        setWidget(row, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(_valpass);
        _uppass = new Button(CAccount.cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updatePassword();
            }
        });
        setWidget(row++, 2, _uppass);
        _uppass.setEnabled(false);

        getFlexCellFormatter().setStyleName(row, 0, "Status");
        getFlexCellFormatter().setColSpan(row, 0, 3);
        setWidget(row++, 0, _status = new Label(CAccount.msgs.editTip()));
    }

    protected void updateRealName ()
    {
        final String oldRealName = _accountInfo.realName;
        _accountInfo.realName = _rname.getText().trim();
        _uprname.setEnabled(false);
        _rname.setEnabled(false);
        CAccount.usersvc.updateAccountInfo(CAccount.ident, _accountInfo, new AsyncCallback() {
            public void onSuccess (Object result) {
                _rname.setEnabled(true);
                _uprname.setEnabled(false);
                setStatus(CAccount.msgs.realNameUpdated());
            }
            public void onFailure (Throwable cause) {
                _rname.setText(_accountInfo.realName = oldRealName);
                _rname.setEnabled(true);
                _uprname.setEnabled(true);
                setError(CAccount.serverError(cause));
            }
        });
    }

    protected void updateEmail ()
    {
        final String email = _email.getText().trim();
        _upemail.setEnabled(false);
        CAccount.usersvc.updateEmail(CAccount.ident, email, new AsyncCallback() {
            public void onSuccess (Object result) {
                CAccount.creds.accountName = email;
                setStatus(CAccount.msgs.emailUpdated());
            }
            public void onFailure (Throwable cause) {
                _upemail.setEnabled(true);
                setError(CAccount.serverError(cause));
            }
        });
    }

    protected void updateEmailPrefs ()
    {
        _upeprefs.setEnabled(false);
        CAccount.usersvc.updateEmailPrefs(CAccount.ident, _whirledEmail.isChecked(),
                                        _announceEmail.isChecked(), new AsyncCallback() {
            public void onSuccess (Object result) {
                _upeprefs.setEnabled(true);
                setStatus(CAccount.msgs.eprefsUpdated());
            }
            public void onFailure (Throwable cause) {
                _upeprefs.setEnabled(true);
                setError(CAccount.serverError(cause));
            }
        });
    }

    protected void updatePassword ()
    {
        final String password = CAccount.md5hex(_password.getText().trim());
        _uppass.setEnabled(false);
        _password.setEnabled(false);
        _confirm.setEnabled(false);
        CAccount.usersvc.updatePassword(CAccount.ident, password, new AsyncCallback() {
            public void onSuccess (Object result) {
                _password.setText("");
                _password.setEnabled(true);
                _confirm.setText("");
                _confirm.setEnabled(true);
                setStatus(CAccount.msgs.passwordUpdated());
            }
            public void onFailure (Throwable cause) {
                _password.setEnabled(true);
                _confirm.setEnabled(true);
                _uppass.setEnabled(true);
                setError(CAccount.serverError(cause));
            }
        });
    }

    protected void configurePermaName ()
    {
        final String pname = _pname.getText().trim();
        _uppname.setEnabled(false);
        _pname.setEnabled(false);
        CAccount.usersvc.configurePermaName(CAccount.ident, pname, new AsyncCallback() {
            public void onSuccess (Object result) {
                CAccount.creds.permaName = pname;
                getFlexCellFormatter().setStyleName(_permaRow, 1, "PermaName");
                setText(_permaRow, 1, pname);
                setText(_permaRow, 2, "");
                setText(_permaRow+1, 0, "");
                setStatus(CAccount.msgs.permaNameConfigured());
            }
            public void onFailure (Throwable cause) {
                _pname.setEnabled(true);
                _uppname.setEnabled(true);
                setError(CAccount.serverError(cause));
            }
        });
    }

    protected void validateRealName ()
    {
        String realName = _rname.getText().trim();
        boolean valid = false;
        if (!_accountInfo.realName.equals(realName)) {
            setStatus(CAccount.msgs.editNameReady());
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
            email.equals(CAccount.creds.accountName)) {
            setStatus("");
        } else {
            setStatus(CAccount.msgs.editEmailReady());
            valid = true;
        }
        _upemail.setEnabled(valid);
    }

    protected void validatePasswords ()
    {
        boolean valid = false;
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (confirm.length() == 0) {
            setError(CAccount.msgs.editMissingConfirm());
        } else if (!password.equals(confirm)) {
            setError(CAccount.msgs.editPasswordMismatch());
        } else {
            setStatus(CAccount.msgs.editPasswordReady());
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
                setError(CAccount.msgs.editPermaInvalid());
                _uppname.setEnabled(false);
                return;
            }
        }

        boolean valid = false;
        if (pname.length() == 0) {
            setStatus("");
        } else if (pname.length() < MemberName.MINIMUM_PERMANAME_LENGTH) {
            setError(CAccount.msgs.editPermaShort());
        } else if (pname.length() > MemberName.MAXIMUM_PERMANAME_LENGTH) {
            setError(CAccount.msgs.editPermaLong());
        } else {
            setStatus(CAccount.msgs.editPermaReady());
            valid = true;
        }
        _uppname.setEnabled(valid);
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

    protected AccountInfo _accountInfo;
    protected int _permaRow;

    protected TextBox _email, _pname, _rname;
    protected CheckBox _whirledEmail, _announceEmail;
    protected PasswordTextBox _password, _confirm;
    protected Button _upemail, _upeprefs, _uppass, _uppname, _uprname;

    protected Label _status;
}
