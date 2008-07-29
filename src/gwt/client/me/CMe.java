//
// $Id$

package client.me;

import client.shell.CShell;

import com.threerings.msoy.world.gwt.WorldServiceAsync;

/**
 * Extends {@link CShell} and provides me-specific services.
 */
public class CMe extends CShell
{
    /** Provides world-related services. */
    public static WorldServiceAsync worldsvc;

    /** Messages used by the me interfaces. */
    public static MeMessages msgs;
}
