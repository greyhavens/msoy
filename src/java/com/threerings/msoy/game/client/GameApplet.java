//
// $Id$

package com.threerings.msoy.game.client;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JApplet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import static com.threerings.msoy.Log.log;

/**
 * Holds the main Java interface to launching (Java) games.
 */
public class GameApplet extends JApplet
{
    @Override // from Applet
    public void init ()
    {
        super.init();

        // set up better logging if possible
        try {
            com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
            OneLineLogFormatter.configureDefaultHandler();
        } catch (SecurityException se) {
            // running in sandbox; no custom logging; no problem!
        }

        log.info("Java: " + System.getProperty("java.version") +
                 ", " + System.getProperty("java.vendor") + ")");

        // configure our server and port
        String server = getParameter("server");
        int port = getIntParameter("port", -1);
        if (server == null || port <= 0) {
            log.warning("Failed to obtain server and port parameters [server=" + server +
                        ", port=" + port + "].");
            return;
        }

        List<URL> urls = new ArrayList<URL>();
        for (URL url : ((URLClassLoader)getClass().getClassLoader()).getURLs()) {
            if (url.getPath().endsWith(".jar")) {
                urls.add(url);
            }
        }

        URLClassLoader loader = URLClassLoader.newInstance(urls.toArray(new URL[urls.size()]), null);
        try {
            Class<?> dclass = loader.loadClass("com.threerings.msoy.game.client.GameWrapper");
            _delegate = dclass.newInstance();
            _init = dclass.getMethod("init", JApplet.class, String.class, Integer.TYPE);
            _start = dclass.getMethod("start", String.class, Integer.TYPE, Integer.TYPE);
            _stop = dclass.getMethod("stop");
            _destroy = dclass.getMethod("destroy");
            _init.invoke(_delegate, this, server, port);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load wrapper class.", e);
        }
    }

    @Override // from Applet
    public void start ()
    {
        super.start();
        String authtok = getParameter("authtoken");
        int gameId = getIntParameter("game_id", -1), gameOid = getIntParameter("game_oid", -1);
        try {
            _start.invoke(_delegate, authtok, gameId, gameOid);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to invoke start() [atok=" + authtok + ", gid=" + gameId +
                    ", goid=" + gameOid + "].", e);
        }
    }

    @Override // from Applet
    public void stop ()
    {
        super.stop();
        try {
            _stop.invoke(_delegate);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to invoke stop().", e);
        }
    }

    @Override // from Applet
    public void destroy ()
    {
        super.destroy();
        try {
            _destroy.invoke(_delegate);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to invoke destroy().", e);
        }
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

    protected Object _delegate;
    protected Method _init, _start, _stop, _destroy;
}
