//
// $Id$

package com.threerings.msoy.game.client;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import javax.swing.JApplet;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.ListUtil;
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

        // don't ask
        List<URL> urls = Lists.newArrayList();
        for (URL url : ((URLClassLoader)getClass().getClassLoader()).getURLs()) {
            if (url.getPath().endsWith(".jar")) {
                urls.add(url);
            }
        }
        URL[] uarray = urls.toArray(new URL[urls.size()]);
        URLClassLoader loader;
        try {
            loader = createSignedClassLoader(uarray);
        } catch (SecurityException se) {
            loader = URLClassLoader.newInstance(uarray, null);
        }
        try {
            Class<?> dclass = loader.loadClass("com.threerings.msoy.game.client.GameWrapper");
            _delegate = dclass.newInstance();
            _init = dclass.getMethod("init", JApplet.class, String.class, Integer.TYPE);
            _start = dclass.getMethod("start", String.class, Integer.TYPE, Integer.TYPE);
            _stop = dclass.getMethod("stop");
            _init.invoke(_delegate, this, server, port);
        } catch (Exception e) {
            log.warning("Failed to load wrapper class.", e);
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
            log.warning("Failed to invoke start() [atok=" + authtok + ", gid=" + gameId +
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
            log.warning("Failed to invoke stop().", e);
        }
    }

    /**
     * Attempts to create a classloader that will grant full permissions to code signed with the
     * same certificates as this class.
     */
    protected URLClassLoader createSignedClassLoader (URL[] urls)
        throws SecurityException
    {
        ProtectionDomain domain = getClass().getProtectionDomain();
        final Certificate[] certs = domain.getCodeSource().getCertificates();
        final PermissionCollection perms = domain.getPermissions();
        return new URLClassLoader(urls, null) {
            protected PermissionCollection getPermissions (CodeSource source) {
                // if the source contains all of GameApplet's certificates, give it that class's
                // permissions; otherwise, fall back on the default permissions
                Certificate[] ocerts = source.getCertificates();
                for (Certificate cert : certs) {
                    if (!ListUtil.contains(ocerts, cert)) {
                        return super.getPermissions(source);
                    }
                }
                return perms;
            }
        };
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
