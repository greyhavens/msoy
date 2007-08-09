//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.MemberInviteResult;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines remote services available to admins.
 */
public interface AdminService extends RemoteService
{
    /** 
     * Grants the given number of invitations to the indicated user set.
     *
     * @param activeSince If null, all users will receive invitations
     */
    public void grantInvitations (WebIdent ident, int numberInvitations, Date activeSince)
        throws ServiceException;

    /**
     * Grants the given number of invitations to the given user.
     */
    public void grantInvitations (WebIdent ident, int numberInvitations, int memberId)
        throws ServiceException;

    /**
     * Fetches a list of players who were invited by inviterId. 
     */
    public MemberInviteResult getPlayerList (WebIdent ident, int inviterId)
        throws ServiceException;
}
