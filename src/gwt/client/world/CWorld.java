//
// $Id$

package client.world;

import client.shell.CShell;

import com.threerings.msoy.world.gwt.WorldServiceAsync;

/**
 * Extends {@link CShell} and provides world-specific services.
 */
public class CWorld extends CShell
{
    /** Provides world-related services. */
    public static WorldServiceAsync worldsvc;

    /** Messages used by the world interfaces. */
    public static WorldMessages msgs;
}
