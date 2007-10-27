//
// $Id$

package client.admin;

import com.threerings.msoy.web.client.AdminServiceAsync;

import client.shell.CShell;

/**
 * Extends {@link CShell} and provides admin-specific services.
 */
public class CAdmin extends CShell
{
    /** Provides admin-related GWT services. */
    public static AdminServiceAsync adminsvc;

    /** Messages used by the admin interfaces. */
    public static AdminMessages msgs;
}
