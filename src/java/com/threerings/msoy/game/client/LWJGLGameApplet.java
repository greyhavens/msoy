//
// $Id$

package com.threerings.msoy.game.client;

import org.lwjgl.util.applet.LWJGLInstaller;

import static com.threerings.msoy.Log.log;

/**
 * Extends {@link GameApplet} to initialize the LWJGL libraries.
 */
public class LWJGLGameApplet extends GameApplet
{
    @Override // from GameApplet
    public void init ()
    {
        super.init();

        // try to load the lwjgl libraries
        try {
            LWJGLInstaller.tempInstall();
        } catch (Exception e) {
            log.warning("Failed to install LWJGL binaries.", e);
        }
    }
}
