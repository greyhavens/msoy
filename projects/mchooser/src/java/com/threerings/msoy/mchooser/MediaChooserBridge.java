//
// $Id$

package com.threerings.msoy.mchooser;

import java.applet.Applet;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Observable;
import java.util.Observer;

import netscape.javascript.JSObject;

/**
 * Bridges between the MediaChooserApplet and a MediaChooser.
 */
public class MediaChooserBridge
    implements Observer
{
    // from interface Observer
    public void update (Observable notused, Object arg)
    {
        final Applet applet = (Applet)arg;
        final Config config = new Config(applet.getParameter("server"), applet.getParameter("media"),
                                         applet.getParameter("auth"), applet.getParameter("mtype"));
        try {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run () {
                    MediaChooser chooser = new MediaChooser(config, JSObject.getWindow(applet));
                    chooser.start();
                    return null;
                }
            });
        } catch (SecurityException se) {
            se.printStackTrace(System.err);
        }
    }
}
