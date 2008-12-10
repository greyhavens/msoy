//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;

import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.item.gwt.ItemDetail;

import com.threerings.msoy.money.data.all.MoneyTransaction;

/**
 * Defines remote services available to admins.
 */
public interface AdminService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/adminsvc";

    /**
     * Return value for an item transaction query.
     */
    public static class ItemTransactionResult
        implements IsSerializable
    {
        /** If requested, the total number of transactions for the item. */
        public int count;

        /** The list of transactions for the requested pagee. */
        public List<MoneyTransaction> transactions;

        /** Member names for each account in the transaction list. */
        public Map<Integer, MemberName> memberNames;
    }

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
     * Gets a page of transactions for a flagged item.
     */
    ItemTransactionResult getItemTransactions (
        ItemIdent iident, int from, int count, boolean needCount)
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

    /**
     * Loads all active promotions.
     */
    List<Promotion> loadPromotions ()
        throws ServiceException;

    /**
     * Adds a new promotion.
     */
    void addPromotion (Promotion promo)
        throws ServiceException;

    /**
     * Deletes the specified promotion.
     */
    void deletePromotion (String promoId)
        throws ServiceException;

    /**
     * Loads all active contests.
     */
    List<Contest> loadContests ()
        throws ServiceException;

    /**
     * Adds a new contest.
     */
    void addContest (Contest contest)
        throws ServiceException;

    /**
     * Edit an existing contest.
     */
    void updateContest (Contest contest)
        throws ServiceException;

    /**
     * Deletes the specified contest.
     */
    void deleteContest (String contestId)
        throws ServiceException;

    /**
     * Returns the supplied server statistics model.
     */
    StatsModel getStatsModel (StatsModel.Type type)
        throws ServiceException;
    
    /**
     * Sets charity info for a particular member, upgrading them to charity status if necessary.
     */
    void setCharityInfo (CharityInfo charityInfo)
        throws ServiceException;
    
    /**
     * Removes charity status from the specified member.  If not currently a charity, does nothing.
     */
    void removeCharityStatus (int memberId)
        throws ServiceException;
}
