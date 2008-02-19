//
// $Id$

package client.account;

import java.util.Date;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.SessionData;

import client.people.SendInvitesPanel;
import client.shell.Application;
import client.shell.Page;
import client.util.DateFields;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountPanel extends FlexTable
{
    public CreateAccountPanel ()
    {
        setCellSpacing(10);
        setStyleName("formPanel");

        int row = 0;
        getFlexCellFormatter().setColSpan(row, 0, 3);
        getFlexCellFormatter().setStyleName(row, 0, "Intro");
        setText(row++, 0, CAccount.msgs.createIntro());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.createEmail());
        setWidget(row, 1, _email = MsoyUI.createTextBox("", -1, 30));
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));
        if (Application.activeInvite != null &&
            Application.activeInvite.inviteeEmail.matches(SendInvitesPanel.EMAIL_REGEX)) {
            // provide the invitation email as the default
            _email.setText(Application.activeInvite.inviteeEmail);
        }
        _email.addKeyboardListener(_validator);
        _email.setFocus(true);
        getFlexCellFormatter().setStyleName(row, 2, "Tip");
        setText(row++, 2, CAccount.msgs.createEmailTip());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.createPassword());
        setWidget(row, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_validator);
        getFlexCellFormatter().setRowSpan(row, 2, 2);
        getFlexCellFormatter().setStyleName(row, 2, "Tip");
        setText(row++, 2, CAccount.msgs.createPasswordTip());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.createConfirm());
        setWidget(row++, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _name.setFocus(true);
            }
        }));
        _confirm.addKeyboardListener(_validator);

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.createDisplayName());
        _name = MsoyUI.createTextBox("", Profile.MAX_DISPLAY_NAME_LENGTH, 30);
        setWidget(row, 1, _name);
        _name.addKeyboardListener(_validator);
        getFlexCellFormatter().setStyleName(row, 2, "Tip");
        setText(row++, 2, CAccount.msgs.createDisplayNameTip());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.createRealName());
        setWidget(row, 1, _rname = MsoyUI.createTextBox("", -1, 30));
        getFlexCellFormatter().setStyleName(row, 2, "Tip");
        setText(row++, 2, CAccount.msgs.createRealNameTip());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, CAccount.msgs.createDateOfBirth());
        setWidget(row, 1, _dateOfBirth = new DateFields());
        getFlexCellFormatter().setStyleName(row, 2, "Tip");
        setText(row++, 2, CAccount.msgs.createDateOfBirthTip());

        getFlexCellFormatter().setColSpan(row, 0, 3);
        getFlexCellFormatter().setStyleName(row, 0, "Status");
        setWidget(row++, 0, _status = new Label(""));

        getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);
        setWidget(row, 1, MsoyUI.createBigButton(CAccount.msgs.createCreate(), new ClickListener() {
            public void onClick (Widget sender) {
                createAccount();
            }
        }));
        setWidget(row++, 2, MsoyUI.createLabel(CAccount.msgs.createAlphaNote(), "Tip"));

        validateData(false);
    }

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        if (_email != null) {
            _email.setFocus(true);
        }
    }

    protected boolean validateData (boolean forceError)
    {
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        String status;
        if (email.length() == 0) {
            status = CAccount.msgs.createMissingEmail();
        } else if (password.length() == 0) {
            status = CAccount.msgs.createMissingPassword();
        } else if (confirm.length() == 0) {
            status = CAccount.msgs.createMissingConfirm();
        } else if (!password.equals(confirm)) {
            status = CAccount.msgs.createPasswordMismatch();
        } else if (name.length() < Profile.MIN_DISPLAY_NAME_LENGTH) {
            status = CAccount.msgs.createNameTooShort(""+Profile.MIN_DISPLAY_NAME_LENGTH);
        } else if (_dateOfBirth.getDate() == null) {
            status = CAccount.msgs.createMissingDoB();
        } else {
            setStatus(CAccount.msgs.createReady());
            return true;
        }

        if (forceError) {
            setError(status);
        } else {
            setStatus(status);
        }
        return false;
    }

    protected void createAccount ()
    {
        if (!validateData(true)) {
            return;
        }

        String[] today = new Date().toString().split(" ");
        String thirteenYearsAgo = "";
        for (int ii = 0; ii < today.length; ii++) {
            if (today[ii].matches("[0-9]{4}")) {
                int year = Integer.valueOf(today[ii]).intValue();
                today[ii] = "" + (year - 13);
            }
            thirteenYearsAgo += today[ii] + " ";
        }

        Date dob = DateFields.toDate(_dateOfBirth.getDate());
        if (new Date(thirteenYearsAgo).compareTo(dob) < 0) {
            setError(CAccount.msgs.createNotThirteen());
            return;
        }

        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim();
        String inviteId = (Application.activeInvite == null) ?
            null : Application.activeInvite.inviteId;
        AccountInfo info = new AccountInfo();
        info.realName = _rname.getText().trim();
        setStatus(CAccount.msgs.creatingAccount());
        CAccount.usersvc.register(DeploymentConfig.version, email, CAccount.md5hex(password), name, 
            _dateOfBirth.getDate(), info, 1, inviteId, new AsyncCallback() {
                public void onSuccess (Object result) {
                    // clear our current token otherwise didLogon() will try to load it
                    Application.setCurrentToken(null);
                    // pass our credentials into the application
                    CAccount.app.didLogon((SessionData)result);
                    // then head to our home
                    Application.go(Page.WORLD, "h");
                }
                public void onFailure (Throwable caught) {
                    setError(CAccount.serverError(caught));
                }
            });
    }

    protected void setStatus (String text)
    {
        _status.removeStyleName("Error");
        _status.setText(text);
    }

    protected void setError (String text)
    {
        _status.addStyleName("Error");
        _status.setText(text);
    }

    protected KeyboardListener _validator = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validateData(false);
                }
            });
        }
    };

    protected TextBox _email, _name, _rname;
    protected PasswordTextBox _password, _confirm;
    protected DateFields _dateOfBirth;
    protected Label _status;
}
