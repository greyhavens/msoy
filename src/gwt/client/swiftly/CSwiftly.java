//
// $Id$

package client.swiftly;

import com.threerings.msoy.web.client.SwiftlyServiceAsync;

import client.shell.CShell;

/**
 * Extends {@link CShell} and provides Swiftly-specific services.
 */
public class CSwiftly extends CShell
{
    /** Provides swiftly-related GWT services. */
    public static SwiftlyServiceAsync swiftlysvc;

    /** Messages used by the swiftly interfaces. */
    public static SwiftlyMessages msgs;
}
