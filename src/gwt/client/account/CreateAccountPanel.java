//
// $Id$

package client.account;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.CaptchaException;
import com.threerings.msoy.web.data.SessionData;

import client.shell.Page;
import client.shell.Session;
import client.shell.TrackingCookie;
import client.ui.DateFields;
import client.ui.MsoyUI;
import client.util.DateUtil;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountPanel extends FlowPanel
{
    public CreateAccountPanel ()
    {
        setStyleName("createAccount");

        FlowPanel loginLink = MsoyUI.createFlowPanel("Login");
        add(loginLink);
        loginLink.add(new Label(CAccount.msgs.createAlreadyMember()));
        PushButton loginButton = MsoyUI.createButton(
            MsoyUI.SHORT_THIN, CAccount.msgs.createLoginLink(),
            Link.createListener(Page.ACCOUNT, "login"));
        loginButton.addClickListener(
            MsoyUI.createTrackingListener("signupLoginButtonClicked", null));
        loginLink.add(loginButton);

        add(MsoyUI.createLabel(CAccount.msgs.createIntro(), "Intro"));
        add(MsoyUI.createLabel(CAccount.msgs.createCoins(), "Coins"));

        add(new LabeledBox(
            CAccount.msgs.createEmail(), _email = new TextBox(), CAccount.msgs.createEmailTip()));
        _email.addKeyboardListener(_onType);
        if (CAccount.activeInvite != null &&
            CAccount.activeInvite.inviteeEmail.matches(MsoyUI.EMAIL_REGEX)) {
            // provide the invitation email as the default
            _email.setText(CAccount.activeInvite.inviteeEmail);
        }

        add(new LabeledBox(
            CAccount.msgs.createRealName(), _rname = new TextBox(),
            CAccount.msgs.createRealNameTip()));
        _rname.addKeyboardListener(_onType);

        add(new LabeledBox(
            CAccount.msgs.createDateOfBirth(), _dateOfBirth = new DateFields(),
            CAccount.msgs.createDateOfBirthTip()));

        add(new LabeledBox(
            CAccount.msgs.createPassword(), _password = new PasswordTextBox(),
            CAccount.msgs.createPasswordTip()));
        _password.addKeyboardListener(_onType);

        add(new LabeledBox(
            CAccount.msgs.createConfirm(), _confirm = new PasswordTextBox(),
            CAccount.msgs.createConfirmTip()));
        _confirm.addKeyboardListener(_onType);

        _name = MsoyUI.createTextBox("", MemberName.MAX_DISPLAY_NAME_LENGTH, -1);
        _name.addKeyboardListener(_onType);
        add(new LabeledBox(
            CAccount.msgs.createDisplayName(), _name, CAccount.msgs.createDisplayNameTip()));

        // optionally add the recaptcha component
        if (hasRecaptchaKey()) {
            add(new LabeledBox(
                CAccount.msgs.createCaptcha(), new HTML("<div id=\"recaptchaDiv\"></div>"), null));
            add(new HTML("<div id=\"recaptchaDiv\"></div>"));
        }

        add(new LabeledBox(CAccount.msgs.createTOS(),
                           _tosBox = new CheckBox(CAccount.msgs.createTOSAgree()), null));

        HorizontalPanel controls = new HorizontalPanel();
        controls.setWidth("500px");
        controls.add(_status = MsoyUI.createLabel("", "Status"));
        controls.add(WidgetUtil.makeShim(10, 10));
        controls.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        ClickListener createGo = new ClickListener() {
            public void onClick (Widget sender) {
                createAccount();
            }
        };
        controls.add(MsoyUI.createButton(MsoyUI.LONG_THICK, CAccount.msgs.createGo(), createGo));
        add(controls);
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        if (_email != null) {
            _email.setFocus(true);
        }
        if (hasRecaptchaKey()) {
            RootPanel.get("recaptchaDiv").add(
                    MsoyUI.createLabel(CAccount.msgs.createCaptchaLoading(), "label"));
            initCaptcha();
        }
    }

    protected void initCaptcha ()
    {
        // our JavaScript is loaded asynchrnously, so there's a possibility that it won't be set up
        // by the time we try to initialize ourselves; in that case we have no recourse but to try
        // again in a short while (there's no way to find out when async JS is loaded)
        if (!createRecaptcha("recaptchaDiv")) {
            new Timer() {
                public void run () {
                    initCaptcha();
                }
            }.schedule(500);
        }
    }

    protected boolean validateData ()
    {
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        String status;
        FocusWidget toFocus = null;
        if (email.length() == 0) {
            status = CAccount.msgs.createMissingEmail();
            toFocus = _email;
        } else if (password.length() == 0) {
            status = CAccount.msgs.createMissingPassword();
            toFocus = _password;
        } else if (confirm.length() == 0) {
            status = CAccount.msgs.createMissingConfirm();
            toFocus = _confirm;
        } else if (!password.equals(confirm)) {
            status = CAccount.msgs.createPasswordMismatch();
            toFocus = _confirm;
        } else if (_dateOfBirth.getDate() == null) {
            status = CAccount.msgs.createMissingDoB();
            // this is not a FocusWidget so we have to handle it specially
            _dateOfBirth.setFocus(true);
        } else if (name.length() < MemberName.MIN_DISPLAY_NAME_LENGTH) {
            status = CAccount.msgs.createNameTooShort(""+MemberName.MIN_DISPLAY_NAME_LENGTH);
            toFocus = _name;
        } else if (!_tosBox.isChecked()) {
            status = CAccount.msgs.createMustAgreeTOS();
        } else if (hasRecaptchaKey() && (getRecaptchaResponse() == null ||
                    getRecaptchaResponse().length() == 0)) {
            status = CAccount.msgs.createMustCaptcha();
            focusRecaptcha();
        } else {
            return true;
        }

        if (toFocus != null) {
            toFocus.setFocus(true);
        }
        setStatus(status);
        return false;
    }

    protected void createAccount ()
    {
        if (!validateData()) {
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

        Date dob = DateUtil.toDate(_dateOfBirth.getDate());
        if (DateUtil.newDate(thirteenYearsAgo).compareTo(dob) < 0) {
            setStatus(CAccount.msgs.createNotThirteen());
            return;
        }

        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim();
        String inviteId = (CAccount.activeInvite == null) ? null : CAccount.activeInvite.inviteId;
        int guestId = CAccount.isGuest() ? CAccount.getMemberId() : 0;
        AccountInfo info = new AccountInfo();
        info.realName = _rname.getText().trim();

        setStatus(CAccount.msgs.creatingAccount());
        String challenge = hasRecaptchaKey() ? getRecaptchaChallenge() : null;
        String response = hasRecaptchaKey() ? getRecaptchaResponse() : null;
        _usersvc.register(
            DeploymentConfig.version, email, CAccount.frame.md5hex(password), name,
            _dateOfBirth.getDate(), null, info, 1, inviteId, guestId, challenge,
            response, TrackingCookie.get(), new AsyncCallback<SessionData>() {
                public void onSuccess (SessionData result) {
                    result.justCreated = true;
                    // pass our credentials into the session (which will trigger a redirect)
                    Session.didLogon(result);
                }
                public void onFailure (Throwable caught) {
                    if (hasRecaptchaKey()) {
                        reloadRecaptcha();
                        if (caught instanceof CaptchaException) {
                            focusRecaptcha();
                        }
                    }
                    setStatus(CAccount.serverError(caught));
                }
            });
    }

    protected void setStatus (String text)
    {
        _status.setText(text);
    }

    protected static Widget makeStep (int step, Widget contents)
    {
        SmartTable table = new SmartTable("Step", 0, 0);
        table.setText(0, 0, step + ".", 1, "Number");
        table.setWidget(0, 1, contents, 1, null);
        return table;
    }

    protected static class LabeledBox extends FlowPanel
    {
        public LabeledBox (String title, Widget contents, String tip)
        {
            setStyleName("Box");
            _tip = new SmartTable("Tip", 0, 0);
            add(title, contents, tip);
        }

        public void add (String title, final Widget contents, final String tip)
        {
            add(MsoyUI.createHTML(title, "Label"));
            add(contents);
            if (contents instanceof SourcesFocusEvents) {
                ((SourcesFocusEvents)contents).addFocusListener(new FocusListener() {
                    public void onFocus (Widget sender) {
                        // we want contents here not sender because of DateFields
                        showTip(contents, tip);
                    }
                    public void onLostFocus (Widget sender) {
                        if (_tip.isAttached()) {
                            remove(_tip);
                        }
                    }
                });
            }
        }

        protected void showTip (Widget trigger, String tip)
        {
            if (!_tip.isAttached() && tip != null) {
                DOM.setStyleAttribute(
                    _tip.getElement(), "left", (trigger.getOffsetWidth()+160) + "px");
                DOM.setStyleAttribute(
                    _tip.getElement(), "top", (trigger.getAbsoluteTop() - getAbsoluteTop() +
                    trigger.getOffsetHeight()/2 - 27) + "px");
                _tip.setText(0, 0, tip);
                add(_tip);
            }
        }

        protected SmartTable _tip;
    }

    protected KeyboardListenerAdapter _onType = new KeyboardListenerAdapter() {
        public void onKeyDown (Widget sender, char keyCode, int modifiers) {
            setStatus("");
        }
    };

    protected static native boolean hasRecaptchaKey () /*-{
        return !(typeof $wnd.recaptchaPublicKey == "undefined");
    }-*/;

    protected static native boolean createRecaptcha (String element) /*-{
        try {
            if ($wnd.Recaptcha != null) {
                $wnd.Recaptcha.create($wnd.recaptchaPublicKey, element, { theme: "white" });
                return true;
            }
        } catch (e) {
            // fall through, return false
        }
        return false;
    }-*/;

    protected static native String getRecaptchaChallenge () /*-{
        return $wnd.Recaptcha.get_challenge();
    }-*/;

    protected static native String getRecaptchaResponse () /*-{
        return $wnd.Recaptcha.get_response();
    }-*/;

    protected static native void focusRecaptcha () /*-{
        $wnd.Recaptcha.focus_response_field();
    }-*/;

    protected static native void reloadRecaptcha () /*-{
        $wnd.Recaptcha.reload();
    }-*/;

    protected TextBox _email, _name, _rname;
    protected PasswordTextBox _password, _confirm;
    protected DateFields _dateOfBirth;
    protected CheckBox _tosBox;
    protected Label _status;

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
