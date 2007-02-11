//
// $Id$

package com.threerings.msoy.admin.client;

import java.awt.EventQueue;
import javax.swing.JApplet;

import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.RunQueue;

import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.msoy.data.MsoyCredentials;

import com.threerings.msoy.admin.data.AdminCodes;
import com.threerings.msoy.admin.util.AdminContext;

import static com.threerings.msoy.Log.log;

/**
 * Provides an admin dashboard client that is connected to a server.
 */
public class AdminApplet extends JApplet
    implements RunQueue
{
    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable run)
    {
        // queue it on up on the awt thread
        EventQueue.invokeLater(run);
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return EventQueue.isDispatchThread();
    }

    @Override // from Applet
    public void init ()
    {
        super.init();

        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        log.info("Java: " + System.getProperty("java.version") +
                 ", " + System.getProperty("java.vendor") + ")");

        // create our various context bits
        _msgmgr = new MessageManager("rsrc.i18n");

        // create our presents client instance
        _client = new Client(null, this);

        // configure our server and port
        String server = null;
        int port = 0;
        try {
            server = getParameter("server");
            port = Integer.parseInt(getParameter("port"));
        } catch (Exception e) {
            // fall through and complain
        }
        if (server == null || port <= 0) {
            log.warning("Failed to obtain server and port parameters " +
                        "[server=" + server + ", port=" + port + "].");
            return;
        }
        log.info("Using [server=" + server + ", port=" + port + "].");
        _client.setServer(server, new int[] { port });

        // create and display our main panel
        add(new AdminPanel(_ctx));
    }

    @Override // from Applet
    public void start ()
    {
        super.start();

        // create our credentials and logon
        MsoyCredentials creds = new MsoyCredentials();
        creds.sessionToken = getParameter("authtoken");
        _client.setCredentials(creds);
        _client.logon();
    }

    @Override // from Applet
    public void stop ()
    {
        super.stop();

        // if we're logged on, log off
        if (_client != null && _client.isLoggedOn()) {
            _client.logoff(true);
        }
    }

    @Override // from Applet
    public void destroy ()
    {
        super.destroy();
        log.info("AdminApplet destroyed.");

        // we need to cope with our threads being destroyed but our classes not being unloaded
        Interval.resetTimer();
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
            return _msgmgr.getBundle(AdminCodes.ADMIN_MSGS).xlate(message);
        }
    }

    protected AdminContext _ctx = new AdminContextImpl();
    protected Config _config = new Config("msoy.admin");
    protected MessageManager _msgmgr;
    protected Client _client;
}
