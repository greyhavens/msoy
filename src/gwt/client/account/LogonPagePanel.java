//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.Frame;
import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * A plain page wrapper for the logon interface.
 */
public class LogonPagePanel extends FlowPanel
{
    public LogonPagePanel ()
    {
        setStyleName("logonPagePanel");

        // add the interface for logging in with Whirled credentials
        add(MsoyUI.createLabel(_msgs.lpLogonHeader(), "Header"));
        PushButton logon = MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.lpSubmit(), null);
        add(new LogonPanel(LogonPanel.Mode.HORIZ, logon));

        // add a link to accout creation
        add(MsoyUI.createLabel(_msgs.lpCreateHeader(), "Header"));
        add(tagButton(MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.lpCreate(),
                                          Link.createListener(Pages.ACCOUNT, "create"))));
    }

    protected Widget tagButton (Widget widget)
    {
        widget.addStyleName("Button");
        return widget;
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
}
