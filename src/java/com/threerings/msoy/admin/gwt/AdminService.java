//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines remote services available to admins.
 */
public interface AdminService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/adminsvc";

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
     * Returns admin information for the specified member.
     */
    public MemberAdminInfo getMemberInfo (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Fetches a list of players who were invited by inviterId.
     */
    public MemberInviteResult getPlayerList (WebIdent ident, int inviterId)
        throws ServiceException;

    /**
     * Sends an email to all registered players who have not opted out of email communications.
     *
     * @param startId the first user id to mail or zero to email all users.
     * @param endId the last user id to mail or zero to email all users.
     *
     * @return a three element array containing the count of successfully sent emails, the count of
     * failures and the count of opt-out accounts.
     */
    public int[] spamPlayers (WebIdent ident, String subject, String body, int startId, int endId)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    public void setIsSupport (WebIdent ident, int memberId, boolean isSupport)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    public List<ABTest> getABTests (WebIdent ident)
        throws ServiceException;

    /**
     * Create a new A/B Test record
     */
    public void createTest (WebIdent ident, ABTest test)
        throws ServiceException;

    /**
     * Update an existing A/B Test record
     */
    public void updateTest (WebIdent ident, ABTest test)
        throws ServiceException;
}
