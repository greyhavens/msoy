//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Defines codes and constants relating to the admin services.
 */
public interface MsoyAdminCodes extends InvocationCodes
{
    /** Identifies our admin message bundle. */
    public static final String ADMIN_MSGS = "admin";

    /** Error returned for a/b tests with duplicate names. */
    public static final String E_AB_TEST_DUPLICATE_NAME = "e.ab_test_duplicate_name";
}
