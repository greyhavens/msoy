//
// $Id$

package client.shell;

import java.util.Date;

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
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;

import client.util.BorderedDialog;
import client.util.DateFields;
import client.util.MsoyUI;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountDialog extends BorderedDialog
{
    public CreateAccountDialog (StatusPanel parent, Invitation invite)
    {
        _parent = parent;
        _invite = invite;
        _header.add(createTitleLabel(CShell.cmsgs.createTitle(), null));
        _footer.add(new Button(CShell.cmsgs.createCreate(), new ClickListener() {
            public void onClick (Widget sender) {
                if (!validateData(true)) {
                    return; // TODO: blink the status message?
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

                if (new Date(thirteenYearsAgo).compareTo(_dateOfBirth.getDate()) < 0) {
                    setError(CShell.cmsgs.createNotThirteen());
                } else {
                    createAccount();
                }
            }
        }));

        FlexTable contents = (FlexTable)_contents;
        int row = 0;
        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Intro");
        contents.setText(row++, 0, CShell.cmsgs.createIntro());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createEmail());
        contents.setWidget(row, 1, _email = new TextBox());
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));
        if (invite != null && invite.inviteeEmail.matches(SendInvitesDialog.EMAIL_REGEX)) {
            // provide the invitation email as the default
            _email.setText(invite.inviteeEmail);
        }
        _email.addKeyboardListener(_validator);
        contents.getFlexCellFormatter().setStyleName(row, 2, "Tip");
        contents.setText(row++, 2, CShell.cmsgs.createEmailTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createPassword());
        contents.setWidget(row, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_validator);
        contents.getFlexCellFormatter().setRowSpan(row, 2, 2);
        contents.getFlexCellFormatter().setStyleName(row, 2, "Tip");
        contents.setText(row++, 2, CShell.cmsgs.createPasswordTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createConfirm());
        contents.setWidget(row++, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _name.setFocus(true);
            }
        }));
        _confirm.addKeyboardListener(_validator);

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createDisplayName());
        contents.setWidget(row, 1, _name = new TextBox());
        _name.addKeyboardListener(_validator);
        contents.getFlexCellFormatter().setStyleName(row, 2, "Tip");
        contents.setText(row++, 2, CShell.cmsgs.createDisplayNameTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createRealName());
        contents.setWidget(row, 1, _rname = new TextBox());
        contents.getFlexCellFormatter().setStyleName(row, 2, "Tip");
        contents.setText(row++, 2, CShell.cmsgs.createRealNameTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createDateOfBirth());
        contents.setWidget(row, 1, _dateOfBirth = new DateFields());
        _dateOfBirth.addKeyboardListenerToFields(_validator);
        contents.getFlexCellFormatter().setStyleName(row, 2, "Tip");
        contents.setText(row++, 2, CShell.cmsgs.createDateOfBirthTip());

        contents.getFlexCellFormatter().setColSpan(row, 0, 3);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Status");
        contents.setWidget(row++, 0, _status = new Label(""));
        _status.setText(CShell.cmsgs.createMissingEmail());
    }

    // @Override // from PopupPanel
    public void show ()
    {
        super.show();
        _email.setFocus(true);
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);
        contents.setStyleName("formDialog");
        return contents;
    }

    protected boolean validateData (boolean forceError)
    {
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        String status;
        if (email.length() == 0) {
            status = CShell.cmsgs.createMissingEmail();
        } else if (password.length() == 0) {
            status = CShell.cmsgs.createMissingPassword();
        } else if (confirm.length() == 0) {
            status = CShell.cmsgs.createMissingConfirm();
        } else if (!password.equals(confirm)) {
            status = CShell.cmsgs.createPasswordMismatch();
        } else if (name.length() == 0) {
            status = CShell.cmsgs.createMissingName();
        } else if (_dateOfBirth.getDate() == null) {
            status = CShell.cmsgs.createMissingDoB();
        } else {
            setStatus(CShell.cmsgs.createReady());
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
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim();
        AccountInfo info = new AccountInfo();
        info.realName = _rname.getText().trim();
        setStatus(CShell.cmsgs.creatingAccount());
        CShell.usersvc.register(DeploymentConfig.version, email, CShell.md5hex(password), name, 
            _dateOfBirth.getDate(), info, 1, _invite, new AsyncCallback() {
                public void onSuccess (Object result) {
                    hide();
                    // override the dialog token with the world
                    Application.setCurrentToken("world");
                    // TODO: display some sort of welcome to whirled business
                    _parent.didLogon((SessionData)result);
                }
                public void onFailure (Throwable caught) {
                    setError(CShell.serverError(caught));
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

    protected StatusPanel _parent;
    protected Invitation _invite;
    protected TextBox _email, _name, _rname;
    protected PasswordTextBox _password, _confirm;
    protected DateFields _dateOfBirth;
    protected Label _status;
}
