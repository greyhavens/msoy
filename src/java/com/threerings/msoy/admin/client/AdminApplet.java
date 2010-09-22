//
// $Id$

package com.threerings.msoy.admin.client;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JApplet;

import com.google.common.collect.Lists;

import static com.threerings.msoy.Log.log;

/**
 * Provides an admin dashboard client that is connected to a server.
 */
public class AdminApplet extends JApplet
{
    @Override // from Applet
    public void init ()
    {
        super.init();

        log.info("Java: " + System.getProperty("java.version") +
                 ", " + System.getProperty("java.vendor") + ")");

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

        // don't ask
        List<URL> urls = Lists.newArrayList();
        for (URL url : ((URLClassLoader)getClass().getClassLoader()).getURLs()) {
            if (url.getPath().endsWith(".jar")) {
                urls.add(url);
            }
        }
        URL[] uarray = urls.toArray(new URL[urls.size()]);
        URLClassLoader loader = URLClassLoader.newInstance(uarray, null);
        try {
            Class<?> dclass = loader.loadClass("com.threerings.msoy.admin.client.AdminWrapper");
            _delegate = dclass.newInstance();
            _init = dclass.getMethod("init", JApplet.class, String.class, Integer.TYPE);
            _start = dclass.getMethod("start", String.class);
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
        try {
            _start.invoke(_delegate, getParameter("authtoken"));
        } catch (Exception e) {
            log.warning("Failed to invoke start().", e);
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

    protected Object _delegate;
    protected Method _init, _start, _stop;
}
