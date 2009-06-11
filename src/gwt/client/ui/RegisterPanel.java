//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.CaptchaException;
import com.threerings.msoy.web.gwt.RegisterInfo;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.LogonPanel;
import client.shell.ShellMessages;
import client.ui.DateFields;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ConversionTrackingUtil;
import client.util.RecaptchaUtil;
import client.util.RegisterUtil;
import client.util.ServiceUtil;
import client.util.StringUtil;

/**
 * Displays a standard registration UI. Designed to be embedded in either the landing page or
 * account-create. Requires that the hosting page have the following JavaScript in index.html:
 *
 * <script id="recaptcha" type="text/javascript"></script>
 */
public class RegisterPanel extends FlowPanel
{
    public RegisterPanel (boolean includeLogon)
    {
        setStyleName("register");
        _includeLogon = includeLogon;
        if (_includeLogon) { // bind action to our logon UI
            LogonPanel.addLogonBehavior(_logemail, _logpass, _doLogon, null);
        }
        setStepOne();
    }

    protected void setStepOne ()
    {
        clear();
        add(MsoyUI.createLabel(_msgs.regiRegister(), "Title"));
        add(MsoyUI.createLabel(_msgs.regiRegisterSub(), "Subtitle"));

        SmartTable regi = new SmartTable(0, 5);
        regi.setText(0, 0, _msgs.regiRegEmail(), 1, "Right");
        regi.setWidget(0, 1, _newemail);
        regi.setText(1, 0, _msgs.regiRegPass(), 1, "Right");
        regi.setWidget(1, 1, _newpass);
        regi.setText(2, 0, _msgs.regiRegBirth(), 1, "Right");
        regi.setWidget(2, 1, _birthday);
        ButtonBase doCreate = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.regiRegGo(), null);
        regi.setWidget(3, 1, doCreate);
        regi.getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
        add(regi);
        for (int row = 0; row < regi.getRowCount(); row++) {
            regi.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }

        if (_includeLogon) {
            add(WidgetUtil.makeShim(15, 15));
            add(MsoyUI.createLabel(_msgs.regiLogon(), "Subtitle"));

            SmartTable logon = new SmartTable(0, 5);
            logon.setText(0, 0, _msgs.regiLogEmail(), 1, "Right");
            logon.setWidget(0, 1, _logemail);
            logon.setText(1, 0, _msgs.regiLogPass(), 1, "Right");
            logon.setWidget(1, 1, _logpass);
            logon.setWidget(2, 1, _doLogon);
            logon.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
            add(logon);
            for (int row = 0; row < regi.getRowCount(); row++) {
                logon.getFlexCellFormatter().setVerticalAlignment(
                    row, 0, HasAlignment.ALIGN_MIDDLE);
            }
        }

        doCreate.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (StringUtil.isBlank(_newemail.getText().trim())) {
                    MsoyUI.errorNear(_msgs.regiFillAll(), _newemail);
                } else if (StringUtil.isBlank(_newpass.getText().trim())) {
                    MsoyUI.errorNear(_msgs.regiFillAll(), _newpass);
                } else if (!RegisterUtil.checkIsThirteen(_birthday)) {
                    MsoyUI.errorNear(_msgs.regiNotThirteen(), _birthday);
                } else {
                    setStepTwo();
                }
            }
        });
    }

    protected void setStepTwo ()
    {
        clear();
        add(MsoyUI.createLabel(_msgs.regiRegister(), "Title"));
        add(MsoyUI.createLabel(_msgs.regiRegisterSub(), "Subtitle"));

        SmartTable regi = new SmartTable(0, 5);

        regi.setText(0, 0, _msgs.regiRegSecurity(), 2, null);
        if (RecaptchaUtil.isEnabled()) {
            regi.setWidget(1, 0, RecaptchaUtil.createDiv("recaptchaDiv"), 2, null);
            DeferredCommand.addCommand(new Command() { // delay init until div is added to DOM
                public void execute () {
                    RecaptchaUtil.init("recaptchaDiv");
                }
            });
        } else {
            regi.setText(1, 0, "CAPTCHA not enabled.", 2, "NoCaptcha");
        }
        final CheckBox tosBox = new CheckBox(_msgs.regiRegTOS(), true);
        regi.setWidget(2, 0, tosBox, 2, null);

        regi.setWidget(3, 0, MsoyUI.createActionLabel(_msgs.regiRegBack(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                setStepOne();
            }
        }));

        ButtonBase doCreate = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.regiRegGo(), null);
        regi.setWidget(3, 1, doCreate);
        regi.getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
        add(regi);

        // create our click callback that handles the actual registation process
        new ClickCallback<WebUserService.RegisterData>(doCreate) {
            @Override protected boolean callService () {
                if (!tosBox.getValue()) {
                    MsoyUI.errorNear(_msgs.regiMustTOS(), tosBox);
                    return false;
                }
                RegisterInfo info = RegisterUtil.createRegInfo(_newemail, _newpass, _birthday);
                // TODO: switch to regi step 3: display "creating account"
                _usersvc.register(DeploymentConfig.version, info, true, this);
                return true;
            }

            @Override protected boolean gotResult (final WebUserService.RegisterData session) {
                setComplete(session);
                return false;
            }

            @Override protected void reportFailure (Throwable cause) {
                if (RecaptchaUtil.isEnabled()) {
                    RecaptchaUtil.reload();
                    if (cause instanceof CaptchaException) {
                        RecaptchaUtil.focus();
                    }
                }
                super.reportFailure(cause);
            }
        };
    }

    protected void setComplete (WebUserService.RegisterData session)
    {
        clear();
        add(MsoyUI.createLabel(_msgs.regiRegistered(), "Title"));
        String email = session.creds.accountName;
        add(MsoyUI.createHTML(_msgs.regiEmailConfirm(email), "Subtitle"));
        add(ConversionTrackingUtil.createAdWordsTracker());
        add(ConversionTrackingUtil.createBeacon(session.entryVector));
    }

    protected boolean _includeLogon;

    protected TextBox _newemail = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
    protected PasswordTextBox _newpass = new PasswordTextBox();
    protected DateFields _birthday = new DateFields();

    protected TextBox _logemail = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
    protected PasswordTextBox _logpass = new PasswordTextBox();
    protected ButtonBase _doLogon = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.regiLogGo(), null);

    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
