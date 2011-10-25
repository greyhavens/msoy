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

public class LandingDjPanel extends SimplePanel
{
    public LandingDjPanel ()
    {
        setStyleName("landingDj");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        this.add(content);

        // logo
        content.add(MsoyUI.createImage("/images/landing/monsterave/logo_big_white.png", "Logo"));

        // login box
        FBLogonPanel fbLogon = new FBLogonPanel();
        fbLogon.setStyleName("Connect");
        content.add(fbLogon);

        // clickable monster ave screenshot opens registration
        ClickHandler showRegistration = new ClickHandler() {
            public void onClick (ClickEvent event) {
                _registerPlaceholder.clear();
                _registerPlaceholder.add(_register);
            }
        };

        // text and copyright
        FlowPanel footer = MsoyUI.createFlowPanel("Footer");
        footer.add(MsoyUI.createHTML(_msgs.djFooter(), "Info"));
        footer.add(LandingCopyright.addFinePrint(MsoyUI.createFlowPanel("Copyright")));
        content.add(footer);
    }

    protected FlowPanel _registerPlaceholder;
    protected RegisterPanel _register;

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
