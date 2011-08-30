//
// $Id$

package com.threerings.msoy.apps.data;

import com.threerings.presents.data.AuthCodes;

import com.threerings.msoy.data.all.GwtAuthCodes;

/**
 * Codes returned by the app servlet.
 */
@com.threerings.util.ActionScript(omit=true)
public interface AppCodes extends AuthCodes, GwtAuthCodes
{
    public static final String E_DAILY_NOTIFICATIONS_NOT_SUPPORTED =
        "e.daily_notifications_not_supported";

    public static final String E_NO_SUCH_APP = "e.no_such_app";

    public static final String E_NO_SUCH_NOTIFICATION = "e.no_such_notification";

    public static final String E_NOTIFICATION_BLANK = "e.notification_blank_text";
}
