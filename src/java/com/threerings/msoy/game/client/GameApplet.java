//
// $Id$

package com.threerings.msoy.game.client;

import java.util.logging.Level;

import com.samskivert.util.Interval;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.presents.client.Client;

import com.threerings.media.FrameManager;
import com.threerings.media.ManagedJApplet;

import static com.threerings.msoy.Log.log;

/**
 * Holds the main Java interface to launching (Java) games.
 */
public class GameApplet extends ManagedJApplet
{
    public void setTitle (String title)
    {
        // TODO
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

        // create our frame manager
        _framemgr = FrameManager.newInstance(this);

        try {
            // create and initialize our client instance
            _client = new GameClient();
            _client.init(this);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create the game client.", e);
            return;
        }

        // configure our server and port
        String server = getParameter("server");
        int port = getIntParameter("port", -1);
        if (server == null || port <= 0) {
            log.warning("Failed to obtain server and port parameters [server=" + server +
                        ", port=" + port + "].");
            return;
        }
        log.info("Using [server=" + server + ", port=" + port + "].");
        _client.getContext().getClient().setServer(server, new int[] { port });
    }

    @Override // from Applet
    public void start ()
    {
        super.start();

        // start up our frame manager
        _framemgr.start();

        // pass our credentials and game information to the client
        _client.start(getParameter("authtoken"), getIntParameter("game_id", -1),
                      getIntParameter("game_oid", -1));
    }

    @Override // from Applet
    public void stop ()
    {
        super.stop();
        _framemgr.stop();

        // if we're logged on, log off
        if (_client != null) {
            Client client = _client.getContext().getClient();
            if (client != null && client.isLoggedOn()) {
                client.logoff(true);
            }
        }
    }

    @Override // from Applet
    public void destroy ()
    {
        super.destroy();
        log.info("GameApplet destroyed.");

        // we need to cope with our threads being destroyed but our classes not
        // being unloaded
        Interval.resetTimer();
    }

    /** Helpy helper function. */
    protected int getIntParameter (String name, int defvalue)
    {
        try {
            return Integer.parseInt(getParameter(name));
        } catch (Exception e) {
            return defvalue;
        }
    }

    protected GameClient _client;
    protected FrameManager _framemgr;
}
