//
// $Id$

package com.threerings.msoy.admin.client;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.util.SwingUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.LogonException;

import com.threerings.admin.client.ConfigEditorPanel;
import com.threerings.admin.data.AdminCodes;

import com.threerings.msoy.admin.util.AdminContext;
import com.threerings.msoy.data.MemberObject;

/**
 * Displays the main admin interface.
 */
public class AdminPanel extends JPanel
{
    public AdminPanel (AdminContext ctx)
    {
        _ctx = ctx;
        _ctx.getClient().addClientObserver(_clobs);

        setBackground(Color.white);
        setLayout(new BorderLayout(5, 5));
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

    protected void clearDisplay ()
    {
        if (_config != null) {
            remove(_config);
            SwingUtil.refresh(this);
            _config = null;
        }
    }

    protected ClientObserver _clobs = new ClientAdapter() {
        public void clientWillLogon (Client client) {
            client.addServiceGroup(AdminCodes.ADMIN_GROUP);
        }
        public void clientDidLogon (Client client) {
            if (((MemberObject)client.getClientObject()).tokens.isAdmin()) {
                add(_config = new ConfigEditorPanel(_ctx));
                _config.setOpaque(false);
            }
            setStatus("m.logged_on");
        }
        public void clientDidLogoff (Client client) {
            clearDisplay();
            setStatus("m.logged_off");
        }
        public void clientFailedToLogon (Client client, Exception cause) {
            setStatus(MessageBundle.compose("m.logon_failed", getNetworkError(cause)));
        }
        public void clientConnectionFailed (Client client, Exception cause) {
            clearDisplay();
            setStatus(MessageBundle.compose("m.logged_off", getNetworkError(cause)));
        }
    };

    protected AdminContext _ctx;
    protected ConfigEditorPanel _config;
    protected JLabel _status;
}
