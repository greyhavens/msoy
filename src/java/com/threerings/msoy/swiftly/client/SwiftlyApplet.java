//
// $Id$

package com.threerings.msoy.swiftly.client;

import static com.threerings.msoy.Log.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
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
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.swiftly.client.controller.PassiveNotifier;
import com.threerings.msoy.swiftly.client.view.GrowlStyleNotifier;
import com.threerings.msoy.swiftly.client.view.SwiftlyWindowView;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientObserver;
import com.threerings.util.IdentUtil;
import com.threerings.util.Name;

public class SwiftlyApplet extends JApplet
    implements ClientObserver, SwiftlyApplication
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
        _ctx.getClient().addClientObserver(this);

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
        _ctx.getClient().setServer(server, new int[] { port });

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
        _ctx.getClient().setCredentials(creds);
        _ctx.getClient().setVersion(String.valueOf(DeploymentConfig.version));
        _ctx.getClient().logon();

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
        if (_ctx.getClient() != null && _ctx.getClient().isLoggedOn()) {
            _ctx.getClient().logoff(true);
        }

        for (ShutdownNotifier notifier : _notifiers) {
            notifier.shuttingDown();
        }
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
                _ctx.getLocationDirector().moveTo((Integer)result);
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
        panel.add(new JLabel(_translator.xlate("m.logged_off")));
        JButton relogon = new JButton(_translator.xlate("m.reconnect"));
        relogon.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getClient().logon();
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

    // from SwiftlyApplication
    public void attachWindow (SwiftlyWindowView window)
    {
        setContentPane(window);
        SwingUtil.refresh(getRootPane());
    }

    // from SwiftlyApplication
    public PassiveNotifier createNotifier ()
    {
        return new GrowlStyleNotifier(getRootPane().getLayeredPane());
    }

    // from SwiftlyApplication
    public void showURL (URL url)
    {
        getAppletContext().showDocument(url);
    }

    // from SwiftlyApplication
    public void addShutdownNotifier (ShutdownNotifier notifier)
    {
        _notifiers.add(notifier);
    }

    /**
     * Display an error message directly onto the applet root panel. Use for errors that prevent
     * the editor to be resolved.
     * @param msg the i18n key to translate for the error message.
     */
    private void appletError (String msg)
    {
        setContentPane(new JLabel(_translator.xlate(msg)));
        SwingUtil.refresh(getRootPane());
    }

    private void createGUI ()
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
    }

    private final Set<ShutdownNotifier> _notifiers = new HashSet<ShutdownNotifier>();

    private final SwiftlyContext _ctx = new SwiftlyContext(this);
    private final Translator _translator = _ctx.getTranslator();
    private int _projectId;
}
