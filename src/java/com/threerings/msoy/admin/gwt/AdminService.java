//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;

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
     * Configures this member's role.
     */
    void setRole (int memberId, WebCreds.Role role)
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
    
    /**
     * Triggers a refresh of bureau launcher information.
     */
    void refreshBureauLauncherInfo ()
        throws ServiceException;

    /**
     * Gets the current info for all connected bureau launchers.
     */
    BureauLauncherInfo[] getBureauLauncherInfo ()
        throws ServiceException;
}
