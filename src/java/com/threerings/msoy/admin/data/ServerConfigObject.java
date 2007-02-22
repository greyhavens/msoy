//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable general server configuration.
 */
public class ServerConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>nonAdminsAllowed</code> field. */
    public static final String NON_ADMINS_ALLOWED = "nonAdminsAllowed";
    // AUTO-GENERATED: FIELDS END

    /** Whether or not to allow non-admins to log on. */
    public boolean nonAdminsAllowed = true;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>nonAdminsAllowed</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNonAdminsAllowed (boolean value)
    {
        boolean ovalue = this.nonAdminsAllowed;
        requestAttributeChange(
            NON_ADMINS_ALLOWED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.nonAdminsAllowed = value;
    }
    // AUTO-GENERATED: METHODS END
}
