//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.underwire.web.client.UnderwireException;

/**
 * Provides some extra methods for the underwire support client for msoy.
 */
public interface SupportService extends RemoteService
{
    /**
     * Sets the greeter flag for a member. Admin only.
     */
    void setGreeter(String authtok, String accountName, boolean greeter)
        throws UnderwireException;

    /**
     * Sets the troublemaker flag for a member. Admin only.
     */
    void setTroublemaker(String authtok, String accountName, boolean troublemaker)
        throws UnderwireException;
}
