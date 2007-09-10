//
// $Id$

package com.threerings.msoy.swiftly.client;

import static com.threerings.msoy.Log.log;

import java.applet.AppletContext;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicBorders;

import org.jvnet.substance.skin.SubstanceBusinessBlackSteelLookAndFeel;

import com.samskivert.servlet.user.Password;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.Name;

public class SwiftlyApplet extends JApplet
    implements RunQueue, ClientObserver
{
    @Override // from JApplet
    public void init()
    {
        // set up better logging if possible
        try {
            OneLineLogFormatter.configureDefaultHandler();
        } catch (SecurityException se) {
            log.info("Running in sandbox. Unable to configure logging.");
        }

        // create our client services
        _client = new Client(null, this);
        _client.addClientObserver(this);
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _locdtr = new LocationDirector(_ctx);
        _occdtr = new OccupantDirector(_ctx);
        _chatdtr = new ChatDirector(_ctx, _msgmgr, "chat");
        _appletContext = getAppletContext();
        _notifier = new GrowlStyleNotifier(getRootPane().getLayeredPane());

        // Get the supplied projectId
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
            // attempt to use a username and password instead
            creds = new MsoyCredentials(new Name(getParameter("username")),
                Password.makeFromClear(getParameter("password")));
        }
        try {
            creds.ident = IdentUtil.getMachineIdentifier();
        } catch (SecurityException se) {
            // no problem, we will have no ident
        }
        // if we got a real ident from the client, mark it as such
        if (creds.ident != null && !creds.ident.matches("S[A-Za-z0-9/+]{32}")) {
            creds.ident = "C" + creds.ident;
        }
        _client.setCredentials(creds);
        _client.setVersion(String.valueOf(DeploymentConfig.version));
        _client.logon();

        // Execute a job on the event-dispatching thread: creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });
        } catch (InterruptedException e) {
            System.err.println("createGUI thread interrupted.");
        } catch (InvocationTargetException e) {
            System.err.println("createGUI couldn't be invoked.");
            Exception ee = (Exception) e.getCause();
            ee.printStackTrace();
            System.out.println("Foo: " + ee);
        }
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
        // we need to cope with our threads being destroyed but our classes not being unloaded
        Interval.resetTimer();
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
    public void clientWillLogon (Client client)
    {
        client.addServiceGroup(SwiftlyCodes.SWIFTLY_GROUP);
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
                appletError(result);
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
        JPanel panel = new JPanel();
        panel.add(new JLabel(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.logged_off")));
        JButton relogon = new JButton(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.reconnect"));
        relogon.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _client.logon();
            }
        });
        panel.add(relogon);
        setContentPane(panel);
        SwingUtil.refresh(getRootPane());
    }

    // from interface ClientObserver
    public void clientFailedToLogon (Client client, Exception cause)
    {
        log.log(Level.WARNING, "Couldn't log on!", cause);
        appletError("m.logon_failed");
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

    /**
     * Display an error message directly onto the applet root panel. Use for errors that prevent
     * the editor to be resolved.
     * @param msg the i18n key to translate for the error message.
     */
    protected void appletError (String msg)
    {
        setContentPane(new JLabel(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, msg)));
        SwingUtil.refresh(getRootPane());
    }

    protected void createGUI ()
    {
        try {
            UIManager.setLookAndFeel(new SubstanceBusinessBlackSteelLookAndFeel());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }
        // TODO: still useful when using substance?
        // let's see how things look without every font being bold
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        // remove the borders from the splitpane so we can add our own later
        UIManager.put("SplitPaneDivider.border", new BasicBorders.MarginBorder());

        // create the progress bar after the UI has been changed
        _progress = new SimpleProgressBar();
    }

    protected class SwiftlyContextImpl implements SwiftlyContext
    {
        public Config getConfig () {
            return _config;
        }

        public Client getClient () {
            return _client;
        }
        public MemberName getMember () {
            return ((MemberObject)getClient().getClientObject()).memberName;
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
        public AppletContext getAppletContext () {
            return _appletContext;
        }

        public void setPlaceView (PlaceView view) {
            setContentPane((SwiftlyEditor)view);
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

        public void showInfoMessage (String message) {
            _notifier.showInfo(message);
        }
        public void showErrorMessage (String message) {
            _notifier.showError(message);
        }

        public void showProgress (int time)
        {
            _progress.showProgress(time);
        }
        public void stopProgress ()
        {
            _progress.stopProgress();
        }
        public SimpleProgressBar getProgressBar ()
        {
            return _progress;
        }
    }

    protected SwiftlyContext _ctx = new SwiftlyContextImpl();
    protected Config _config = new Config("swiftly");
    protected MessageManager _msgmgr;

    protected Client _client;
    protected LocationDirector _locdtr;
    protected OccupantDirector _occdtr;
    protected ChatDirector _chatdtr;
    protected AppletContext _appletContext;
    protected PassiveNotifier _notifier;
    protected SimpleProgressBar _progress;

    protected int _projectId;

    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";
}
