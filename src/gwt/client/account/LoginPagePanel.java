//
// $Id: CreateAccountPanel.java 9950 2008-07-22 18:32:35Z mdb $

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;

import client.shell.LogonPanel;
import client.ui.MsoyUI;

/**
 * A plain page wrapper for the login interface.
 */
public class LoginPagePanel extends FlowPanel
{
    public LoginPagePanel ()
    {
        setStyleName("loginPagePanel");
        PushButton loginButton = MsoyUI.createButton(
            MsoyUI.MEDIUM_THIN, _msgs.loginPageSubmit(), null);
        loginButton.addStyleName("LoginButton");
        add(new LogonPanel(true, loginButton));
        add(loginButton);
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
}
