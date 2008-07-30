//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;

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
    void grantInvitations (WebIdent ident, int numberInvitations, Date activeSince)
        throws ServiceException;

    /**
     * Grants the given number of invitations to the given user.
     */
    void grantInvitations (WebIdent ident, int numberInvitations, int memberId)
        throws ServiceException;

    /**
     * Returns admin information for the specified member.
     */
    MemberAdminInfo getMemberInfo (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Fetches a list of players who were invited by inviterId.
     */
    MemberInviteResult getPlayerList (WebIdent ident, int inviterId)
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
    int[] spamPlayers (WebIdent ident, String subject, String body, int startId, int endId)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    void setIsSupport (WebIdent ident, int memberId, boolean isSupport)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    List<ABTest> getABTests (WebIdent ident)
        throws ServiceException;

    /**
     * Create a new A/B Test record
     */
    void createTest (WebIdent ident, ABTest test)
        throws ServiceException;

    /**
     * Update an existing A/B Test record
     */
    void updateTest (WebIdent ident, ABTest test)
        throws ServiceException;

    /**
     * Fetches the first 'count' items flagged as mature or copyright in the database.
     */
    List<ItemDetail> getFlaggedItems (WebIdent ident, int count)
        throws ServiceException;

    /**
     * Deletes an item and notifies people who care with the given message.  If the item is listed
     * in the catalog, also delists it and deletes any clones.
     */
    Integer deleteItemAdmin (WebIdent ident, ItemIdent item, String subject, String body)
        throws ServiceException;
}
