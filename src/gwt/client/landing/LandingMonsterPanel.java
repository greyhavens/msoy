//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.FBLogonPanel;
import client.shell.LogonPanel.ForgotPasswordDialog;
import client.shell.LogonPanel;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.RegisterPanel;

/**
 * Our main landing page.
 */
public class LandingMonsterPanel extends SimplePanel
{
    public LandingMonsterPanel ()
    {
        setStyleName("landingMonsterAve");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        this.add(content);

        // logo
        content.add(MsoyUI.createImage("/images/landing/monsterave/logo_big_white.png", "Logo"));

        // login box
        FlowPanel loginBox = MsoyUI.createFlowPanel("LoginBox");
        final TextBox emailTextBox = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
        PasswordTextBox logpass = new PasswordTextBox();
        SmartTable logon = new SmartTable("Login", 0, 5);
        logon.setText(0, 0, _msgs.landingLogEmail(), 1, "Right");
        logon.setWidget(0, 1, emailTextBox);
        logon.setText(1, 0, _msgs.landingLogPass(), 1, "Right");
        logon.setWidget(1, 1, logpass);
        loginBox.add(logon);
        ButtonBase logonButton = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.landingLogGo(),
            null);
        LogonPanel.addLogonBehavior(emailTextBox, logpass, logonButton, null);
        loginBox.add(LogonPanel.newForgotPassword(emailTextBox.getText().trim()));
        loginBox.add(logonButton);
        FBLogonPanel fbLogon = new FBLogonPanel();
        fbLogon.setStyleName("Connect");
        loginBox.add(fbLogon);
        content.add(loginBox);

        // clickable monster ave screenshot opens registration
        ClickHandler showRegistration = new ClickHandler() {
            public void onClick (ClickEvent event) {
                _registerPlaceholder.clear();
                _registerPlaceholder.add(_register);
            }
        };
        Image screenshot = MsoyUI.createActionImage(
            "/images/landing/monsterave/monsterave_screenshot.jpg", showRegistration);
        screenshot.setStyleName("Screenshot");
        content.add(screenshot);

        // clickable join now button will be replaced by register box
        _registerPlaceholder = MsoyUI.createFlowPanel("RegisterPlaceholder");
        Image joinButton = MsoyUI.createActionImage("/images/landing/monsterave/join_button.png",
            showRegistration);
        joinButton.setStyleName("JoinButton");
        _registerPlaceholder.add(joinButton);
        content.add(_registerPlaceholder);

        // register panel (initially hidden)
        _register = new RegisterPanel() {
            protected void addHeader (boolean complete) {
                if (complete) {
                    add(MsoyUI.createLabel(_msgs.landingRegistered(), "Title"));
                } else {
                    add(MsoyUI.createLabel(_msgs.landingRegister(), "Title"));
                    add(MsoyUI.createLabel(_msgs.landingRegisterSub(), "Subtitle"));
                }
            }
        };

        // meet create play image
        content.add(MsoyUI.createImage("/images/landing/monsterave/meet_create_play.jpg",
            "Meetcreateplay"));

        // text and copyright
        FlowPanel footer = MsoyUI.createFlowPanel("Footer");
        footer.add(MsoyUI.createHTML(_msgs.monsterAveFooter(), "Info"));
        footer.add(LandingCopyright.addFinePrint(MsoyUI.createFlowPanel("Copyright")));
        content.add(footer);
    }

    protected FlowPanel _registerPlaceholder;
    protected RegisterPanel _register;

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
