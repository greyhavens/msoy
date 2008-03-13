//
// $Id$

package client.account;

import java.util.Date;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
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
import client.util.RoundBox;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountPanel extends VerticalPanel
{
    public CreateAccountPanel ()
    {
        setStyleName("createAccount");

        add(MsoyUI.createLabel(CAccount.msgs.createIntro(), "Intro"));

        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);

        box.add(new LabeledBox(CAccount.msgs.createEmail(),
                               _email = MsoyUI.createTextBox("", -1, 30),
                               CAccount.msgs.createEmailTip()));
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

        box.add(WidgetUtil.makeShim(10, 10));

        box.add(new LabeledBox(CAccount.msgs.createPassword(), _password = new PasswordTextBox(),
                               CAccount.msgs.createConfirm(), _confirm = new PasswordTextBox(),
                               CAccount.msgs.createPasswordTip()));
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_validator);
        _confirm.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _name.setFocus(true);
            }
        }));
        _confirm.addKeyboardListener(_validator);

        add(box);
        add(WidgetUtil.makeShim(15, 15));

        box = new RoundBox(RoundBox.DARK_BLUE);

        _name = MsoyUI.createTextBox("", Profile.MAX_DISPLAY_NAME_LENGTH, 30);
        _name.addKeyboardListener(_validator);
        box.add(new LabeledBox(CAccount.msgs.createDisplayName(), _name,
                               CAccount.msgs.createDisplayNameTip()));

        box.add(WidgetUtil.makeShim(10, 10));
        box.add(new LabeledBox(CAccount.msgs.createRealName(),
                               _rname = MsoyUI.createTextBox("", -1, 30),
                               CAccount.msgs.createRealNameTip()));

        box.add(WidgetUtil.makeShim(10, 10));
        box.add(new LabeledBox(CAccount.msgs.createDateOfBirth(), _dateOfBirth = new DateFields(),
                               CAccount.msgs.createDateOfBirthTip()));
        add(box);
        add(WidgetUtil.makeShim(15, 15));

        add(_status = MsoyUI.createLabel("", "Status"));
        add(WidgetUtil.makeShim(15, 15));

        // setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        add(MsoyUI.createButton(MsoyUI.LONG_THICK, CAccount.msgs.createCreate(), new ClickListener() {
            public void onClick (Widget sender) {
                createAccount();
            }
        }));

        Label slurp = new Label();
        add(slurp);
        setCellHeight(slurp, "100%");

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
        CAccount.usersvc.register(
            DeploymentConfig.version, email, CAccount.md5hex(password), name, _dateOfBirth.getDate(),
            info, 1, inviteId, Application.activeGuestId, new AsyncCallback() {
            public void onSuccess (Object result) {
                // clear our current token otherwise didLogon() will try to load it
                Application.setCurrentToken(null);
                // pass our credentials into the application
                CAccount.app.didLogon((SessionData)result);
                // then head to our me page
                Application.go(Page.ME, "");
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

    protected static class LabeledBox extends FlowPanel
        implements FocusListener
    {
        public LabeledBox (String title, Widget contents, String tip)
        {
            setStyleName("Box");
            add(MsoyUI.createLabel(title, "Label"));
            add(contents);
            if (contents instanceof SourcesFocusEvents) {
                ((SourcesFocusEvents)contents).addFocusListener(this);
            }
            _tip = MsoyUI.createLabel(tip, "Tip");
        }

        public LabeledBox (String title1, Widget contents1,
                           String title2, Widget contents2, String tip)
        {
            this(title1, contents1, tip);
            add(WidgetUtil.makeShim(3, 3));
            add(MsoyUI.createLabel(title2, "Label"));
            add(contents2);
            if (contents2 instanceof SourcesFocusEvents) {
                ((SourcesFocusEvents)contents2).addFocusListener(this);
            }
        }

        // from interface FocusListener
        public void onFocus (Widget sender) {
            if (!_tip.isAttached()) {
                add(_tip);
                DOM.setStyleAttribute(
                    _tip.getElement(), "left", (sender.getOffsetWidth()+10) + "px");
                DOM.setStyleAttribute(
                    _tip.getElement(), "top", (sender.getOffsetHeight()-20) + "px");
            }
        }

        // from interface FocusListener
        public void onLostFocus (Widget sender) {
            if (_tip.isAttached()) {
                remove(_tip);
            }
        }

        protected Label _tip;
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
