//
// $Id$

package com.threerings.msoy.client;

import java.applet.Applet;

import netscape.javascript.JSObject;

/**
 * An applet that lets GWT know that Java is up and running.
 */
public class HowdyPardner extends Applet
{
    @Override // from Applet
    public void init () {
        JSObject win = JSObject.getWindow(this);
        win.call("howdyPardner", new Object[0]);
    }
}
