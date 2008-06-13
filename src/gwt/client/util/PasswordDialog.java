package client.util;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.Widget;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.SessionData;

import client.shell.CShell;

public class PasswordDialog extends BorderedDialog
    implements AsyncCallback
{
    public PasswordDialog (String message)
    {
        // TODO: Unsuck layout
        HorizontalPanel panel = new HorizontalPanel();

        final PasswordTextBox password = new PasswordTextBox();

        panel.add(new Label(message));
        //panel.add(WidgetUtil.makeShim(20, 10));
        panel.add(password);

        ClickListener click = new ClickListener() {
            public void onClick(Widget w) {
                hide();
                CShell.usersvc.login(DeploymentConfig.version, CShell.creds.accountName,
                        CShell.md5hex(password.getText()), 1, PasswordDialog.this);
            }
        };

        // TODO: i18n
        panel.add(new Button("OK", click));
        setWidget(panel);
    }

    public void onSuccess (Object result)
    {
        MsoyUI.info("Yep");
        CShell.app.didLogon((SessionData)result);
    }

    public void onFailure (Throwable caught)
    {
        MsoyUI.info("Nope");
    }

    //protected String _account;
}

