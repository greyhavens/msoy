//
// $Id$

package client.world;

import client.shell.CShell;

import com.threerings.msoy.room.gwt.WorldServiceAsync;

/**
 * Extends {@link CShell} and provides world-specific services.
 */
public class CWorld extends CShell
{
    /** Provides world-related services. */
    public static WorldServiceAsync worldsvc;
}
