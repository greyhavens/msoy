//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;

import client.shell.LogonPanel;
import client.ui.MsoyUI;

/**
 * A plain page wrapper for the logon interface.
 */
public class LogonPagePanel extends FlowPanel
{
    public LogonPagePanel ()
    {
        setStyleName("logonPagePanel");
        PushButton logonButton = MsoyUI.createButton(
            MsoyUI.MEDIUM_THIN, _msgs.logonPageSubmit(), null);
        logonButton.addStyleName("LogonButton");
        add(new LogonPanel(true, logonButton));
        add(logonButton);
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
}
