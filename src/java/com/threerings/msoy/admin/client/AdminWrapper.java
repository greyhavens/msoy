//
// $Id$

package com.threerings.msoy.admin.client;

import javax.swing.JApplet;

import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;

import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.DeploymentConfig;

import com.threerings.msoy.admin.data.MsoyAdminCodes;
import com.threerings.msoy.admin.util.AdminContext;

import static com.threerings.msoy.Log.log;

/**
 * Some byzantine shit to work around bullshit AppletClassLoader problems.
 */
public class AdminWrapper
{
    public void init (JApplet applet, String server, int port)
    {
        // create our various context bits
        _msgmgr = new MessageManager("rsrc.i18n");

        // create our presents client instance
        _client = new Client(null, RunQueue.AWT);

        log.info("Using [server=" + server + ", port=" + port + "].");
        _client.setServer(server, new int[] { port });

        // create and display our main panel
        applet.add(new AdminPanel(_ctx));
    }

    public void start (String authToken)
    {
        // create our credentials and logon
        WorldCredentials creds = new WorldCredentials();
        creds.sessionToken = authToken;
        _client.setCredentials(creds);
        _client.setVersion(DeploymentConfig.version);
        _client.logon();
    }

    public void stop ()
    {
        // if we're logged on, log off
        if (_client != null && _client.isLoggedOn()) {
            _client.logoff(true);
        }
    }

    protected class AdminContextImpl implements AdminContext
    {
        public Config getConfig () {
            return _config;
        }
        public Client getClient () {
            return _client;
        }
        public DObjectManager getDObjectManager () {
            return _client.getDObjectManager();
        }
        public MessageManager getMessageManager () {
            return _msgmgr;
        }
        public String xlate (String message) {
            return _msgmgr.getBundle(MsoyAdminCodes.ADMIN_MSGS).xlate(message);
        }
    }

    protected AdminContext _ctx = new AdminContextImpl();
    protected Config _config = new Config("msoy.admin");
    protected MessageManager _msgmgr;
    protected Client _client;
}
