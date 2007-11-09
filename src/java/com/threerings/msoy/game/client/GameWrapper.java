//
// $Id$

package com.threerings.msoy.game.client;

import java.util.logging.Level;

import java.awt.Component;
import java.awt.Window;
import javax.swing.JApplet;
import javax.swing.JRootPane;

import com.samskivert.util.Interval;

import com.threerings.media.FrameManager;

import com.threerings.presents.client.Client;

import static com.threerings.msoy.Log.log;

/**
 * Some byzantine shit to work around bullshit AppletClassLoader problems.
 */
public class GameWrapper
{
    // from interface GameApplet.Delegate
    public void init (final JApplet applet, String server, int port)
    {
        log.info("Creating frame manager...");

        // create our frame manager
        _framemgr = FrameManager.newInstance(new FrameManager.ManagedRoot() {
            public void init (FrameManager fmgr) {
                // TODO?
            }
            public Window getWindow () {
                Component parent = applet.getParent();
                while (!(parent instanceof Window) && parent != null) {
                    parent = parent.getParent();
                }
                return (Window)parent;
            }
            public JRootPane getRootPane() {
                return applet.getRootPane();
            }
        });

        try {
            log.info("Creating client...");
            // create and initialize our client instance
            _client = new GameClient();
            log.info("Initializing client...");
            _client.init(applet, _framemgr);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create the game client.", e);
            return;
        }

        log.info("Using [server=" + server + ", port=" + port + "].");
        _client.getContext().getClient().setServer(server, new int[] { port });

        log.info("GameApplet finished init.");
    }

    // from interface GameApplet.Delegate
    public void start (String authToken, int gameId, int gameOid)
    {
        log.info("GameApplet starting.");

        // start up our frame manager
        _framemgr.start();

        // pass our credentials and game information to the client
        _client.start(authToken, gameId, gameOid);

        log.info("GameApplet started.");
    }

    // from interface GameApplet.Delegate
    public void stop ()
    {
        log.info("GameApplet stopping.");

        _framemgr.stop();

        // if we're logged on, log off
        if (_client != null) {
            Client client = _client.getContext().getClient();
            if (client != null && client.isLoggedOn()) {
                client.logoff(true);
            }
        }

        log.info("GameApplet stopped.");
    }

    // from interface GameApplet.Delegate
    public void destroy ()
    {
        log.info("GameApplet destroying.");

        // we need to cope with our threads being destroyed but our classes not
        // being unloaded
        Interval.resetTimer();

        log.info("GameApplet destroyed.");
    }

    protected GameClient _client;
    protected FrameManager _framemgr;
}
