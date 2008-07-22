//
// $Id$

package com.threerings.msoy.admin.util;

import com.threerings.util.MessageManager;

import com.threerings.presents.util.PresentsContext;

/**
 * Provides services to the admin dashboard applet.
 */
public interface AdminContext extends PresentsContext
{
    /** Provides access to translation messages. */
    MessageManager getMessageManager ();

    /** Translates a message from the admin message bundle. */
    String xlate (String message);
}
