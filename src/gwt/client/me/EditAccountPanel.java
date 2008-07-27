//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.AccountInfo;

import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays account information, allows twiddling.
 */
public class EditAccountPanel extends SmartTable
{
    public EditAccountPanel ()
    {
        setCellSpacing(10);
        setStyleName("editAccount");

        _usersvc.getAccountInfo(CMe.ident, new MsoyCallback<AccountInfo>() {
            public void onSuccess (AccountInfo info) {
                init(info);
            }
        });
    }

    protected void init (AccountInfo accountInfo)
    {
        _accountInfo = accountInfo;

        int row = 0;

        // configure or display permaname interface
        if (CMe.creds.permaName == null) {
            setText(row++, 0, CMe.msgs.editPickPermaNameHeader(), 3, "Header");

            setText(row, 0, CMe.msgs.editPermaName(), 1, "rightLabel");
            setWidget(row, 1, _pname = new TextBox());
            _pname.addKeyboardListener(_valpname);
            _uppname = new Button(_cmsgs.set(), new ClickListener() {
                public void onClick (Widget widget) {
                    configurePermaName();
                }
            });
            _uppname.setEnabled(false);
            setWidget(_permaRow = row++, 2, _uppname);

            setHTML(row++, 0, CMe.msgs.editPermaNameTip(), 3, "Tip");

        } else {
            setText(row, 0, CMe.msgs.editPermaName(), 1, "rightLabel");
            setText(row++, 1, CMe.creds.permaName, 1, "PermaName");
        }

        // configure email address interface
        setText(row++, 0, CMe.msgs.editEmailHeader(), 3, "Header");

        setText(row, 0, CMe.msgs.editEmail(), 1, "rightLabel");
        setWidget(row, 1, _email = new TextBox());
        _email.setText(CMe.creds.accountName);
        _email.addKeyboardListener(_valemail);
        _upemail = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmail();
            }
        });
        _upemail.setEnabled(false);
        setWidget(row++, 2, _upemail);

        // configure email preferences interface
        setText(row++, 0, CMe.msgs.editEPrefsHeader(), 3, "Header");

        setText(row, 0, CMe.msgs.editWhirledMailEmail(), 1, "rightLabel");
        RowPanel bits = new RowPanel();
        bits.add(_whirledEmail = new CheckBox());
        bits.add(MsoyUI.createLabel(CMe.msgs.editWhirledMailEmailTip(), "tipLabel"));
        setWidget(row++, 1, bits, 2, null);
        _whirledEmail.setChecked(_accountInfo.emailWhirledMail);

        setText(row, 0, CMe.msgs.editAnnounceEmail(), 1, "rightLabel");
        bits = new RowPanel();
        bits.add(_announceEmail = new CheckBox());
        bits.add(MsoyUI.createLabel(CMe.msgs.editAnnounceEmailTip(), "tipLabel"));
        setWidget(row++, 1, bits, 2, null);
        _announceEmail.setChecked(_accountInfo.emailAnnouncements);

        _upeprefs = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateEmailPrefs();
            }
        });
        setWidget(row++, 2, _upeprefs);

        // configure real name interface
        setText(row++, 0, CMe.msgs.editRealNameHeader(), 3, "Header");

        setText(row, 0, CMe.msgs.editRealName(), 1, "rightLabel");
        setWidget(row, 1, _rname = new TextBox());
        _rname.setText(_accountInfo.realName);
        _rname.addKeyboardListener(_valrname);
        _uprname = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updateRealName();
            }
        });
        _uprname.setEnabled(false);
        setWidget(row++, 2, _uprname);

        setHTML(row++, 0, CMe.msgs.editRealNameTip(), 3, "Tip");

        // configure password interface
        setText(row++, 0, CMe.msgs.editPasswordHeader(), 3, "Header");

        setText(row, 0, CMe.msgs.editPassword(), 1, "rightLabel");
        setWidget(row++, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_valpass);

        setText(row, 0, CMe.msgs.editConfirm(), 1, "rightLabel");
        setWidget(row, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(_valpass);
        _uppass = new Button(_cmsgs.update(), new ClickListener() {
            public void onClick (Widget widget) {
                updatePassword();
            }
        });
        setWidget(row++, 2, _uppass);
        _uppass.setEnabled(false);

        // TEMP: toggle client height
        setText(row++, 0, "Toggle Client Height", 3, "Header");

        setText(row, 0, "You can toggle the client between the standard height or the " +
                "full height of our browser. Note: this feature is experimental and you " +
                "may experience slowness if you use the client in full-height mode. " +
                "This is also not currently saved across sessions.", 2, "Tip");
        setWidget(row++, 1, new Button("Toggle", new ClickListener() {
            public void onClick (Widget widget) {
                FlashClients.toggleClientHeight();
            }
        }));
        // END TEMP

        setWidget(row++, 0, _status = new Label(CMe.msgs.editTip()), 3, "Status");
    }

    protected void updateRealName ()
    {
        final String oldRealName = _accountInfo.realName;
        _accountInfo.realName = _rname.getText().trim();
        _uprname.setEnabled(false);
        _rname.setEnabled(false);
        _usersvc.updateAccountInfo(CMe.ident, _accountInfo, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _rname.setEnabled(true);
                _uprname.setEnabled(false);
                setStatus(CMe.msgs.realNameUpdated());
            }
            public void onFailure (Throwable cause) {
                _rname.setText(_accountInfo.realName = oldRealName);
                _rname.setEnabled(true);
                _uprname.setEnabled(true);
                setError(CMe.serverError(cause));
            }
        });
    }

    protected void updateEmail ()
    {
        final String email = _email.getText().trim();
        _upemail.setEnabled(false);
        _usersvc.updateEmail(CMe.ident, email, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                CMe.creds.accountName = email;
                setStatus(CMe.msgs.emailUpdated());
            }
            public void onFailure (Throwable cause) {
                _upemail.setEnabled(true);
                setError(CMe.serverError(cause));
            }
        });
    }

    protected void updateEmailPrefs ()
    {
        _upeprefs.setEnabled(false);
        _usersvc.updateEmailPrefs(CMe.ident, _whirledEmail.isChecked(),
                                  _announceEmail.isChecked(), new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _upeprefs.setEnabled(true);
                setStatus(CMe.msgs.eprefsUpdated());
            }
            public void onFailure (Throwable cause) {
                _upeprefs.setEnabled(true);
                setError(CMe.serverError(cause));
            }
        });
    }

    protected void updatePassword ()
    {
        final String password = CMe.md5hex(_password.getText().trim());
        _uppass.setEnabled(false);
        _password.setEnabled(false);
        _confirm.setEnabled(false);
        _usersvc.updatePassword(CMe.ident, password, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                _password.setText("");
                _password.setEnabled(true);
                _confirm.setText("");
                _confirm.setEnabled(true);
                setStatus(CMe.msgs.passwordUpdated());
            }
            public void onFailure (Throwable cause) {
                _password.setEnabled(true);
                _confirm.setEnabled(true);
                _uppass.setEnabled(true);
                setError(CMe.serverError(cause));
            }
        });
    }

    protected void configurePermaName ()
    {
        final String pname = _pname.getText().trim();
        _uppname.setEnabled(false);
        _pname.setEnabled(false);
        _usersvc.configurePermaName(CMe.ident, pname, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                CMe.creds.permaName = pname;
                getFlexCellFormatter().setStyleName(_permaRow, 1, "PermaName");
                setText(_permaRow, 1, pname);
                setText(_permaRow, 2, "");
                setText(_permaRow+1, 0, "");
                setStatus(CMe.msgs.permaNameConfigured());
            }
            public void onFailure (Throwable cause) {
                _pname.setEnabled(true);
                _uppname.setEnabled(true);
                setError(CMe.serverError(cause));
            }
        });
    }

    protected void validateRealName ()
    {
        String realName = _rname.getText().trim();
        boolean valid = false;
        if (!_accountInfo.realName.equals(realName)) {
            setStatus(CMe.msgs.editNameReady());
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
            email.equals(CMe.creds.accountName)) {
            setStatus("");
        } else {
            setStatus(CMe.msgs.editEmailReady());
            valid = true;
        }
        _upemail.setEnabled(valid);
    }

    protected void validatePasswords ()
    {
        boolean valid = false;
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (confirm.length() == 0) {
            setError(CMe.msgs.editMissingConfirm());
        } else if (!password.equals(confirm)) {
            setError(CMe.msgs.editPasswordMismatch());
        } else {
            setStatus(CMe.msgs.editPasswordReady());
            valid = true;
        }
        _uppass.setEnabled(valid);
    }

    protected void validatePermaName ()
    {
        // extract the permaname, but also show the user exactly what we're using, since
        // we lowercase and trim it.
        int cursor = _pname.getCursorPos();
        String raw = _pname.getText();
        String pname = raw.trim();
        cursor -= raw.indexOf(pname);
        pname = pname.toLowerCase();
        _pname.setText(pname);
        _pname.setCursorPos(cursor);

        // now check it for legality
        for (int ii = 0; ii < pname.length(); ii++) {
            char c = pname.charAt(ii);
            if ((ii == 0 && !Character.isLetter(c)) ||
                (!Character.isLetter(c) && !Character.isDigit(c) && c != '_')) {
                setError(CMe.msgs.editPermaInvalid());
                _uppname.setEnabled(false);
                return;
            }
        }

        boolean valid = false;
        if (pname.length() == 0) {
            setStatus("");
        } else if (pname.length() < MemberName.MINIMUM_PERMANAME_LENGTH) {
            setError(CMe.msgs.editPermaShort());
        } else if (pname.length() > MemberName.MAXIMUM_PERMANAME_LENGTH) {
            setError(CMe.msgs.editPermaLong());
        } else {
            setStatus(CMe.msgs.editPermaReady());
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
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    validateRealName();
                }
            });
        }
    };

    protected KeyboardListener _valemail = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    validateEmail();
                }
            });
        }
    };

    protected KeyboardListener _valpass = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    validatePasswords();
                }
            });
        }
    };

    protected KeyboardListener _valpname = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.addCommand(new Command() {
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

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
