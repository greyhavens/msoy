//
// $Id$

package client.swiftly;

import com.threerings.msoy.web.client.SwiftlyServiceAsync;

import client.shell.ShellContext;


/**
 * Extends {@link ShellContext} and provides game-specific services.
 */
public class SwiftlyContext extends ShellContext
{
    /** Provides swiftly-related GWT services. */
    public SwiftlyServiceAsync swiftlysvc;

    /** Messages used by the swiftly interfaces. */
    public SwiftlyMessages msgs;
}
