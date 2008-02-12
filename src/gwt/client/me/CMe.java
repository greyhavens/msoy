//
// $Id$

package client.me;

import client.msgs.CMsgs;

import com.threerings.msoy.web.client.WorldServiceAsync;

/**
 * Extends {@link CShell} and provides me-specific services.
 */
public class CMe extends CMsgs
{
    /** Provides world-related services. */
    public static WorldServiceAsync worldsvc;

    /** Messages used by the me interfaces. */
    public static MeMessages msgs;
}
