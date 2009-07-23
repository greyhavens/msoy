//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;

import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.facebook.gwt.NotificationStatus;

import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.item.gwt.ItemDetail;

import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.gwt.BroadcastHistory;

/**
 * Defines remote services available to admins.
 */
@RemoteServiceRelativePath(value=AdminService.REL_PATH)
public interface AdminService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/adminsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + AdminService.ENTRY_POINT;

    /**
     * Return value for an item transaction query.
     */
    public static class ItemTransactionResult extends PagedResult<MoneyTransaction>
        implements IsSerializable
    {
        /** Member names for each account in the transaction list. */
        public Map<Integer, MemberName> memberNames;
    }

    /**
     * Return value for an item deletion.
     */
    public static class ItemDeletionResult
        implements IsSerializable
    {
        /** Number of deleted items. */
        public int deletionCount;

        /** Number of refund transactions created. */
        public int refunds;

        /** Number of items reclaimed. */
        public int reclaimCount;

        /** Number of items that cause an error during reclaim. */
        public int reclaimErrors;

        /** Number of listings associated with the item that were removed. */
        public int listings;
    }

    /**
     * Return value when querying flagged items.
     */
    public static class ItemFlagsResult extends PagedResult<ItemFlag>
        implements IsSerializable
    {
        /** Details for flagged items. */
        public Map<ItemIdent, ItemDetail> items;

        /** Member names of flagging members. */
        public Map<Integer, MemberName> memberNames;
    }

    /**
     * Return value when querying the history of broadcast messages.
     */
    public static class BroadcastHistoryResult extends PagedResult<BroadcastHistory>
    {
        /** Member names of broadcasting members. */
        public Map<Integer, MemberName> memberNames;
    }

    /**
     * Returns admin information for the specified member.
     */
    MemberAdminInfo getMemberInfo (int memberId)
        throws ServiceException;

    /**
     * Configures this member's role.
     */
    void setRole (int memberId, WebCreds.Role role)
        throws ServiceException;

    /**
     * Resets this member's humanity to the default value.
     */
    int resetHumanity (int memberId)
        throws ServiceException;

    /**
     * Configures this member's display name.
     */
    void setDisplayName (int memberId, String name)
        throws ServiceException;

    /**
     * Configures this member's validated flag.
     */
    void setValidated (int memberId, boolean value)
        throws ServiceException;

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    List<ABTest> getABTests ()
        throws ServiceException;

    /**
     * Returns the summary for the specified A/B test.
     */
    ABTestSummary getABTestSummary (int testId)
        throws ServiceException;

    /**
     * Fetches a page of item flags from the database and some more information to display them.
     */
    ItemFlagsResult getItemFlags (int from, int count, boolean needCount)
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
    ItemDeletionResult deleteItemAdmin (ItemIdent item, String subject, String body)
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
     * Updates an existing promotion.
     */
    void updatePromotion (Promotion promo)
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

    /**
     * Requests that the server be rebooted in the specified number of minutes. If the value is
     * zero the server will be rebooted immediately, if it is negative, a pending reboot will be
     * aborted and a new regularly scheduled reboot scheduled.
     */
    void scheduleReboot (int minutes, String message)
        throws ServiceException;

    /**
     * Returns a list of the node names of our peer servers.
     */
    Set<String> getPeerNodeNames ()
        throws ServiceException;

    /**
     * Restarts the Panopticon logging client.
     */
    void restartPanopticon (Set<String> nodeNames)
        throws ServiceException;

    /**
     * Gets a page of user paid broadcast message history.
     */
    BroadcastHistoryResult getBroadcastHistory (int offset, int count, boolean needCount);

    /**
     * Returns a summary of recent Whirled "entries".
     */
    List<EntrySummary> summarizeEntries ()
        throws ServiceException;

    /**
     * Loads all facebook templates.
     */
    List<FacebookTemplate> loadFacebookTemplates ()
        throws ServiceException;

    /**
     * Saves changes to the given facebook templates and deletes the set of templates specified
     * by the given codes.
     */
    void updateFacebookTemplates (Set<FacebookTemplate> changed, Set<FacebookTemplate> removed)
        throws ServiceException;

    /**
     * Sends a Facebook notification to all our Facebook users after the given delay in minutes.
     * Users who have removed the application are not addressed.
     */
    void sendFacebookNotification (String id, String text, int delay)
        throws ServiceException;

    /**
     * Gets the statuses of all registered facebook notifications.
     */
    List<NotificationStatus> getFacebookNotificationStatuses ()
        throws ServiceException;

    /**
     * Gets the saved facebook notifications.
     */
    List<FacebookNotification> loadFacebookNotifications ()
        throws ServiceException;

    /**
     * Updates or adds a new facebook notification.
     */
    void saveFacebookNotification (FacebookNotification notif)
        throws ServiceException;

    /**
     * Delets a facebook notification.
     */
    void deleteFacebookNotification (String id)
        throws ServiceException;
}
