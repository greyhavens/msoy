//
// $Id$

package com.threerings.msoy.web.gwt;

/**
 * Error codes used by our web services.
 */
public interface ServiceCodes
{
    /** An error code returned to clients when a service cannot be performed because of some
     * internal server error that we couldn't explain in any meaningful way (things like null
     * pointer exceptions). */
    public static final String E_INTERNAL_ERROR = "e.internal_error";

    /** An error code returned to clients when a service cannot be performed because the requesting
     * client does not have the proper access. */
    public static final String E_ACCESS_DENIED = "e.access_denied";
}
