//
// $Id$

package com.threerings.msoy.fora.gwt;

import com.threerings.msoy.web.data.ServiceCodes;

/**
 * Codes and constants relating to the issue services.
 */
public interface IssueCodes extends ServiceCodes
{
    /** An error code reported by the issue services. */
    public static final String E_ISSUE_CLOSED = "e.issue_closed";

    /** An error code reported by the issue services. */
    public static final String E_ISSUE_CLOSE_NO_OWNER = "e.issue_close_no_owner";

    /** An error code reported by the issue services. */
    public static final String E_ISSUE_CLOSE_NOT_OWNER = "e.issue_close_not_owner";
}
