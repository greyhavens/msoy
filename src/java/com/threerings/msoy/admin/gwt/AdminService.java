//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.gwt.ServiceException;

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
     * Get the specified page of affiliate mappings.
     */
    PagedResult<AffiliateMapping> getAffiliateMappings (int start, int count, boolean needTotal)
        throws ServiceException;

    /**
     * Set the specified affiliate to map to the specified memberId.
     */
    void mapAffiliate (String affiliate, int memberId)
        throws ServiceException;

    /**
     * Grants the given number of invitations to the indicated user set.
     *
     * @param activeSince If null, all users will receive invitations
     */
    void grantInvitations (int numberInvitations, Date activeSince)
        throws ServiceException;

    /**
     * Grants the given number of invitations to the given user.
     */
    void grantInvitations (int numberInvitations, int memberId)
        throws ServiceException;

    /**
     * Returns admin information for the specified member.
     */
    MemberAdminInfo getMemberInfo (int memberId)
        throws ServiceException;

    /**
     * Fetches a list of players who were invited by inviterId.
     */
    MemberInviteResult getPlayerList (int inviterId)
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
    int[] spamPlayers (String subject, String body, int startId, int endId)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    void setIsSupport (int memberId, boolean isSupport)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    List<ABTest> getABTests ()
        throws ServiceException;

    /**
     * Create a new A/B Test record
     */
    void createTest (ABTest test)
        throws ServiceException;

    /**
     * Update an existing A/B Test record
     */
    void updateTest (ABTest test)
        throws ServiceException;

    /**
     * Fetches the first 'count' items flagged as mature or copyright in the database.
     */
    List<ItemDetail> getFlaggedItems (int count)
        throws ServiceException;

    /**
     * Deletes an item and notifies people who care with the given message.  If the item is listed
     * in the catalog, also delists it and deletes any clones.
     */
    Integer deleteItemAdmin (ItemIdent item, String subject, String body)
        throws ServiceException;
}
