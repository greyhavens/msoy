//
// $Id$

package com.threerings.msoy.fora.gwt;

import com.threerings.msoy.web.data.ServiceCodes;

/**
 * Codes and constants relating to the forum services.
 */
public interface ForumCodes extends ServiceCodes
{
    /** An error code reported by the forum services. */
    public static final String E_INVALID_GROUP = "e.invalid_group";

    /** An error code reported by the forum services. */
    public static final String E_INVALID_THREAD = "e.invalid_thread";

    /** An error code reported by the forum services. */
    public static final String E_INVALID_MESSAGE = "e.invalid_message";
}
