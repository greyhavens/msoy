//
// $Id$

package client.admin;

import client.editem.CEditem;

import com.threerings.msoy.web.client.AdminServiceAsync;

/**
 * Extends {@link CMsgs} and provides admin-specific services.
 */
public class CAdmin extends CEditem
{
    /** Provides admin-related GWT services. */
    public static AdminServiceAsync adminsvc;

    /** Messages used by the admin interfaces. */
    public static AdminMessages msgs;
}
