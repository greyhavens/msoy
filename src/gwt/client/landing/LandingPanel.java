//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.CaptchaException;
import com.threerings.msoy.web.gwt.RegisterInfo;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.LogonPanel;
import client.ui.DateFields;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ConversionTrackingUtil;
import client.util.RecaptchaUtil;
import client.util.RegisterUtil;
import client.util.ServiceUtil;
import client.util.StringUtil;

/**
 * Our main landing page.
 */
public class LandingPanel extends FlowPanel
{
    public LandingPanel ()
    {
        setStyleName("landing");

        // create a UI explaining briefly what Whirled is
        FlowPanel explain = MsoyUI.createFlowPanel("Explain");
        explain.add(MsoyUI.createLabel(_msgs.landingIntro(), "Title"));
        explain.add(MsoyUI.createLabel(_msgs.landingIntroSub(), "Subtitle"));
        final SimplePanel video = new SimplePanel();
        video.setStyleName("Video");
        video.setWidget(MsoyUI.createActionImage("/images/landing/play_screen.png",
                                                 _msgs.landingClickToStart(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                video.setWidget(WidgetUtil.createFlashContainer(
                    "preview", "/images/landing/landing_movie.swf", 208, 154, null));
            }
        }));
        explain.add(video);

        // bind action to our logon UI
        LogonPanel.addLogonBehavior(_logemail, _logpass, _doLogon, null);

        // create a UI for registering and logging on
        final FlowPanel register = MsoyUI.createFlowPanel("Regbits");
        setRegiStepOne(register);

        // wrap all that up in two columns with header and background and whatnot
        add(MsoyUI.createImage("/images/account/register_banner.jpg", null));
        SmartTable content = new SmartTable("Content", 0, 20);
        content.setWidget(0, 0, explain);
        content.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_LEFT);
        content.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        content.setWidget(0, 1, register);
        content.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        content.setWidget(1, 0, LandingCopyright.addFinePrint(new FlowPanel()), 2, null);
        content.getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
        add(content);
        add(MsoyUI.createImage("/images/account/create_bg_bot.png", null));
    }

    protected void setRegiStepOne (final FlowPanel register)
    {
        register.clear();
        register.add(MsoyUI.createLabel(_msgs.landingRegister(), "Title"));
        register.add(MsoyUI.createLabel(_msgs.landingRegisterSub(), "Subtitle"));

        SmartTable regi = new SmartTable(0, 5);
        regi.setText(0, 0, _msgs.landingRegEmail(), 1, "Right");
        regi.setWidget(0, 1, _newemail);
        regi.setText(1, 0, _msgs.landingRegPass(), 1, "Right");
        regi.setWidget(1, 1, _newpass);
        regi.setText(2, 0, _msgs.landingRegBirth(), 1, "Right");
        regi.setWidget(2, 1, _birthday);
        ButtonBase doCreate = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.landingRegGo(), null);
        regi.setWidget(3, 1, doCreate);
        regi.getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
        register.add(regi);
        for (int row = 0; row < regi.getRowCount(); row++) {
            regi.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }

        register.add(WidgetUtil.makeShim(15, 15));
        register.add(MsoyUI.createLabel(_msgs.landingLogon(), "Subtitle"));

        SmartTable logon = new SmartTable(0, 5);
        logon.setText(0, 0, _msgs.landingLogEmail(), 1, "Right");
        logon.setWidget(0, 1, _logemail);
        logon.setText(1, 0, _msgs.landingLogPass(), 1, "Right");
        logon.setWidget(1, 1, _logpass);
        logon.setWidget(2, 1, _doLogon);
        logon.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
        register.add(logon);
        for (int row = 0; row < regi.getRowCount(); row++) {
            logon.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }

        doCreate.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (StringUtil.isBlank(_newemail.getText().trim())) {
                    MsoyUI.errorNear(_msgs.landingFillAll(), _newemail);
                } else if (StringUtil.isBlank(_newpass.getText().trim())) {
                    MsoyUI.errorNear(_msgs.landingFillAll(), _newpass);
                } else if (!RegisterUtil.checkIsThirteen(_birthday)) {
                    MsoyUI.errorNear(_msgs.landingNotThirteen(), _birthday);
                } else {
                    setRegiStepTwo(register);
                }
            }
        });
    }

    protected void setRegiStepTwo (final FlowPanel register)
    {
        register.clear();
        register.add(MsoyUI.createLabel(_msgs.landingRegister(), "Title"));
        register.add(MsoyUI.createLabel(_msgs.landingRegisterSub(), "Subtitle"));

        SmartTable regi = new SmartTable(0, 5);

        regi.setText(0, 0, _msgs.landingRegSecurity(), 2, null);
        if (RecaptchaUtil.isEnabled()) {
            regi.setWidget(1, 0, RecaptchaUtil.createDiv("recaptchaDiv"), 2, null);
            RecaptchaUtil.init("recaptchaDiv");
        } else {
            regi.setText(1, 0, "CAPTCHA not enabled.", 2, "NoCaptcha");
        }
        final CheckBox tosBox = new CheckBox(_msgs.landingRegTOS(), true);
        regi.setWidget(2, 0, tosBox, 2, null);

        regi.setWidget(3, 0, MsoyUI.createActionLabel(_msgs.landingRegBack(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                setRegiStepOne(register);
            }
        }));

        ButtonBase doCreate = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.landingRegGo(), null);
        regi.setWidget(3, 1, doCreate);
        regi.getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
        register.add(regi);

        // create our click callback that handles the actual registation process
        new ClickCallback<WebUserService.RegisterData>(doCreate) {
            @Override protected boolean callService () {
                if (!tosBox.isChecked()) {
                    MsoyUI.errorNear(_msgs.landingMustTOS(), tosBox);
                    return false;
                }
                RegisterInfo info = RegisterUtil.createRegInfo(_newemail, _newpass, _birthday);
                // TODO: switch to regi step 3: display "creating account"
                _usersvc.register(DeploymentConfig.version, info, true, this);
                return true;
            }

            @Override protected boolean gotResult (final WebUserService.RegisterData session) {
                setRegiComplete(register, session);
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

    protected void setRegiComplete (final FlowPanel register, WebUserService.RegisterData session)
    {
        register.clear();
        register.add(MsoyUI.createLabel(_msgs.landingRegistered(), "Title"));
        String email = session.creds.accountName;
        register.add(MsoyUI.createHTML(_msgs.landingEmailConfirm(email), "Subtitle"));
        register.add(ConversionTrackingUtil.createAdWordsTracker());
        register.add(ConversionTrackingUtil.createBeacon(session.entryVector));
    }

    protected TextBox _newemail = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
    protected PasswordTextBox _newpass = new PasswordTextBox();
    protected DateFields _birthday = new DateFields();

    protected TextBox _logemail = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
    protected PasswordTextBox _logpass = new PasswordTextBox();
    protected ButtonBase _doLogon = MsoyUI.createButton(
        MsoyUI.SHORT_THIN, _msgs.landingLogGo(), null);

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
