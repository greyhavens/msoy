//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.EventQueue;
import java.util.logging.Level;
import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import static com.threerings.msoy.Log.log;

public class SwiftlyApplet extends JApplet
    implements RunQueue, ClientObserver
{
    @Override // from JApplet
    public void init()
    {
        // set up better logging
        OneLineLogFormatter.configureDefaultHandler();

        // create our client services
        _client = new Client(null, this);
        _client.addClientObserver(this);
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _locdtr = new LocationDirector(_ctx);
        _occdtr = new OccupantDirector(_ctx);
        _chatdtr = new ChatDirector(_ctx, _msgmgr, "chat");

        // Load the authentication token and configure the XML-RPC connection
        _projectId = Integer.parseInt(getParameter("projectId"));

        // configure our server and port
        String server = getParameter("server");
        int port = Integer.parseInt(getParameter("port"));
        if (server == null || port <= 0) {
            log.warning("Failed to obtain server and port parameters " +
                        "[server=" + server + ", port=" + port + "].");
            return;
        }
        log.info("Using [server=" + server + ", port=" + port + "].");
        _client.setServer(server, new int[] { port });

        // create our credentials and logon
        MsoyCredentials creds = new MsoyCredentials();
        creds.sessionToken = getParameter("authtoken");
        if (StringUtil.isBlank(creds.sessionToken)) {
            log.warning("Missing session token parameter (authtoken).");
            return;
        }
        creds.ident = IdentUtil.getMachineIdentifier();
        // if we got a real ident from the client, mark it as such
        if (creds.ident != null && !creds.ident.matches("S[A-Za-z0-9/+]{32}")) {
            creds.ident = "C" + creds.ident;
        }
        _client.setCredentials(creds);
        _client.logon();

        // Execute a job on the event-dispatching thread: creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });

        } catch (Exception e) {
            System.err.println("createGUI didn't successfully complete.");
            Exception ee = (Exception) e.getCause();
            ee.printStackTrace();
            System.out.println("Foo: " + ee);
        }
    }

    @Override // from Applet
    public void destroy ()
    {
        super.destroy();
        // we need to cope with our threads being destroyed but our classes not being unloaded
        Interval.resetTimer();
    }

    public SwiftlyEditor getEditor ()
    {
        return _editor;
    }

    /**
     * Saves a file element on the backend. Creates the element if it doesn't already exist. 
     * @param element the {@link PathElement} to save.
     */
    public void saveElement (PathElement element)
    {
        // TODO save the file element on the backend
        // TODO show a progress bar in the status bar while Saving...
    }

    /**
     * Deletes a file element on the backend.
     * @param element the {@link PathElement} to delete.
     */
    public void deleteElement (PathElement element)
    {
        // TODO delete the file element on the backend
    }

    /**
     * Renames a file element on the backend.
     * @param element the {@link PathElement} to rename.
     */
    public void renameElement (PathElement element, String newName)
    {
        // TODO rename the element on the backend
    }

    // from interface RunQueue
    public void postRunnable (Runnable r)
    {
        EventQueue.invokeLater(r);
    }

    // from interface RunQueue
    public boolean isDispatchThread ()
    {
        return EventQueue.isDispatchThread();
    }

    // from interface ClientObserver
    public void clientDidLogon (Client client)
    {
        SwiftlyService ssvc = (SwiftlyService)client.requireService(SwiftlyService.class);
        ssvc.enterProject(client, _projectId, new SwiftlyService.ResultListener() {
            public void requestProcessed (Object result) {
                _locdtr.moveTo((Integer)result);
            }
            public void requestFailed (String result) {
                log.warning("Oh crap, it's too hot today. " + result);
                // TODO: warning
            }
        });
    }

    // from interface ClientObserver
    public void clientObjectDidChange (Client client)
    {
        // nada
    }

    // from interface ClientObserver
    public boolean clientWillLogoff (Client client)
    {
        return true; // okie doke
    }

    // from interface ClientObserver
    public void clientDidLogoff (Client client)
    {
        setContentPane(new JLabel("You got logged the fuck off!"));
        SwingUtil.refresh(getRootPane());
    }

    // from interface ClientObserver
    public void clientFailedToLogon (Client client, Exception cause)
    {
        // TODO: freak out!
        log.log(Level.WARNING, "Couldn't log on!", cause);
    }

    // from interface ClientObserver
    public void clientConnectionFailed (Client client, Exception cause)
    {
        // TODO: freak out!
        log.log(Level.WARNING, "Connection failed!", cause);
    }

    // from interface ClientObserver
    public void clientDidClear (Client client)
    {
        // nada
    }

    protected void createGUI ()
    {
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // TODO the gtk L&F breaks some bits. Just use the default L&F for now.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }
    }

    protected class SwiftlyContextImpl implements SwiftlyContext
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
        public LocationDirector getLocationDirector () {
            return _locdtr;
        }
        public OccupantDirector getOccupantDirector () {
            return _occdtr;
        }
        public ChatDirector getChatDirector () {
            return _chatdtr;
        }

        public void setPlaceView (PlaceView view) {
            setContentPane(_editor = (SwiftlyEditor)view);
            SwingUtil.refresh(getRootPane());
        }
        public void clearPlaceView (PlaceView view) {
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public String xlate (String bundle, String message) {
            MessageBundle mb = getMessageManager().getBundle(bundle);
            return (mb == null) ? message : mb.xlate(message);
        }
    }

    protected SwiftlyContext _ctx = new SwiftlyContextImpl();
    protected Config _config = new Config("swiftly");
    protected MessageManager _msgmgr;

    protected Client _client;
    protected LocationDirector _locdtr;
    protected OccupantDirector _occdtr;
    protected ChatDirector _chatdtr;
    protected SwiftlyEditor _editor;

    protected int _projectId;

    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";
}
