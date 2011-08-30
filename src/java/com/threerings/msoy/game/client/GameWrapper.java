//
// $Id$

package com.threerings.msoy.game.client;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JApplet;
import javax.swing.JRootPane;

import com.threerings.presents.client.Client;

import com.threerings.media.FrameManager;

import static com.threerings.msoy.Log.log;

/**
 * Some byzantine shit to work around bullshit AppletClassLoader problems.
 */
public class GameWrapper
{
    // from interface GameApplet.Delegate
    public void init (final JApplet applet, String server, int port)
    {
        // create our frame manager
        _framemgr = FrameManager.newInstance(new FrameManager.ManagedRoot() {
            public void init (FrameManager fmgr) {
                // don't need it
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
            // create and initialize our client instance
            _client = new GameClient();
            _client.init(applet, _framemgr);
        } catch (Exception e) {
            log.warning("Failed to create the game client.", e);
            return;
        }

        log.info("Using [server=" + server + ", port=" + port + "].");
        _client.getContext().getClient().setServer(server, new int[] { port });
    }

    // from interface GameApplet.Delegate
    public void start (String authToken, int gameId, int gameOid)
    {
        // start up our frame manager
        _framemgr.start();

        // pass our credentials and game information to the client
        _client.start(authToken, gameId, gameOid);
    }

    // from interface GameApplet.Delegate
    public void stop ()
    {
        _framemgr.stop();

        // if we're logged on, log off
        if (_client != null) {
            Client client = _client.getContext().getClient();
            if (client != null && client.isLoggedOn()) {
                client.logoff(true);
            }
        }
    }

    protected GameClient _client;
    protected FrameManager _framemgr;
}
