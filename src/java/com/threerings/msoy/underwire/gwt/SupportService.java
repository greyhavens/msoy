//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.underwire.gwt.MsoyAccount.SocialStatus;
import com.threerings.underwire.web.client.UnderwireException;

/**
 * Provides some extra methods for the underwire support client for msoy.
 */
public interface SupportService extends RemoteService
{
    public static final String ENTRY_POINT = "/undersvc";
    
    /**
     * Sets the social status for a member. Admin only.
     */
    void setSocialStatus (int memberId, SocialStatus status)
        throws UnderwireException;
}
