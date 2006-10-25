//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.MemberGName;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Defines member-specific services available to the GWT/AJAX web client.
 */
public interface MemberService extends RemoteService
{
    /**
     * Look up a member by id and return their current name.
     */
    public MemberGName getName (int memberId)
        throws ServiceException;
}
