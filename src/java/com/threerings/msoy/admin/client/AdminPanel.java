//
// $Id$

package com.threerings.msoy.admin.client;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.LogonException;

import com.threerings.msoy.admin.util.AdminContext;

/**
 * Displays the main admin interface.
 */
public class AdminPanel extends JPanel
{
    public AdminPanel (AdminContext ctx)
    {
        _ctx = ctx;
        _ctx.getClient().addClientObserver(_clobs);

        setLayout(new BorderLayout());
        add(_status = new JLabel(""), BorderLayout.SOUTH);
        setStatus("m.logging_on");
    }

    /**
     * Translates and displays the supplied status message.
     */
    public void setStatus (String message)
    {
        _status.setText(_ctx.xlate(message));
    }

    protected String getNetworkError (Exception cause)
    {
        return (cause instanceof LogonException) ? cause.getMessage() :
            MessageBundle.taint(cause.getMessage());
    }

    protected ClientObserver _clobs = new ClientAdapter() {
        public void clientDidLogon (Client client) {
            // TODO: display dashboard
            setStatus("m.logged_on");
        }
        public void clientDidLogoff (Client client) {
            // TODO: clear dashboard
            setStatus("m.logged_off");
        }
        public void clientFailedToLogon (Client client, Exception cause) {
            setStatus(MessageBundle.compose("m.logon_failed", getNetworkError(cause)));
        }
        public void clientConnectionFailed (Client client, Exception cause) {
            setStatus(MessageBundle.compose("m.logged_off", getNetworkError(cause)));
        }
    };

    protected AdminContext _ctx;
    protected JLabel _status;
}
