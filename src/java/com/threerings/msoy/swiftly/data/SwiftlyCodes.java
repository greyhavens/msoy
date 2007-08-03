//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Swiftly services.
 */
public interface SwiftlyCodes extends InvocationCodes
{
    /** Defines our invocation service group. */
    public static final String SWIFTLY_GROUP = "msoy.swiftly";

    /** The identifier for our translation message bundle. */
    public static final String SWIFTLY_MSGS = "swiftly";

    /** An error code returned by the Swiftly services. */
    public static final String E_NO_SUCH_PROJECT = "m.no_such_project";

    /** An error code returned by the Swiftly services. */
    public static final String E_PROJECT_NAME_EXISTS = "m.project_name_exists";
}
