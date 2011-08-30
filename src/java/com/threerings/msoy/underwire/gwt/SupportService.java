//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.underwire.web.client.UnderwireException;

import com.threerings.msoy.underwire.gwt.MsoyAccount.SocialStatus;

/**
 * Provides some extra methods for the underwire support client for msoy.
 */
@RemoteServiceRelativePath(SupportService.REL_PATH)
public interface SupportService extends RemoteService
{
    public static final String ENTRY_POINT = "/undersvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + SupportService.ENTRY_POINT;

    /**
     * Sets the social status for a member. Admin only.
     */
    void setSocialStatus (int memberId, SocialStatus status)
        throws UnderwireException;
}
