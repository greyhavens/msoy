//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.admin.gwt.AdminService.BroadcastHistoryResult;
import com.threerings.msoy.admin.gwt.AdminService.ItemDeletionResult;
import com.threerings.msoy.admin.gwt.AdminService.ItemFlagsResult;
import com.threerings.msoy.admin.gwt.AdminService.ItemTransactionResult;

import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.facebook.gwt.FacebookTemplate;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * The asynchronous (client-side) version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The asynchronous version of {@link AdminService#getMemberInfo}.
     */
    void getMemberInfo (int memberId, AsyncCallback<MemberAdminInfo> callback);

    /**
     * The asynchronous version of {@link AdminService#setRole}.
     */
    void setRole (int memberId, WebCreds.Role role, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#resetHumanity}.
     */
    void resetHumanity (int memberId, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link AdminService#setDisplayName}.
     */
    void setDisplayName (int memberId, String name, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#setValidated}.
     */
    void setValidated (int memberId, boolean valid, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getABTests}.
     */
    void getABTests (AsyncCallback<List<ABTest>> callback);

    /**
     * The asynchronous version of {@link AdminService#getABTestSummary}.
     */
    void getABTestSummary (int testId, AsyncCallback<ABTestSummary> callback);

    /**
     * The asynchronous version of {@link AdminService#getItemFlags}.
     */
    void getItemFlags (int start, int count, boolean needCount,
        AsyncCallback<ItemFlagsResult> callback);

    /**
     * The asynchronous version of {@link AdminService#getItemTransactions}.
     */
    void getItemTransactions (ItemIdent iident, int from, int count, boolean needCount,
        AsyncCallback<ItemTransactionResult> callback);

    /**
     * The asynchronous version of {@link AdminService#deleteItemAdmin}.
     */
    void deleteItemAdmin (ItemIdent item, String subject, String body,
                          AsyncCallback<ItemDeletionResult> callback);

    /**
     * The asynchronous version of {@link AdminService#getBureauLauncherInfo}.
     */
    void getBureauLauncherInfo (AsyncCallback<BureauLauncherInfo[]> callback);

    /**
     * The asynchronous version of {@link AdminService#loadPromotions}.
     */
    void loadPromotions (AsyncCallback<List<Promotion>> callback);

    /**
     * The asynchronous version of {@link AdminService#addPromotion}.
     */
    void addPromotion (Promotion promo, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#updatePromotion}.
     */
    void updatePromotion (Promotion promo, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#deletePromotion}.
     */
    void deletePromotion (String promoId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#loadContests}.
     */
    void loadContests (AsyncCallback<List<Contest>> callback);

    /**
     * The asynchronous version of {@link AdminService#addContest}.
     */
    void addContest (Contest contest, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#updateContest}.
     */
    void updateContest (Contest contest, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#deleteContest}.
     */
    void deleteContest (String contestId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getStatsModel}.
     */
    void getStatsModel (StatsModel.Type type, AsyncCallback<StatsModel> callback);

    /**
     * The asynchronous version of {@link AdminService#setCharityInfo}.
     */
    void setCharityInfo (CharityInfo charityInfo, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#removeCharityStatus}.
     */
    void removeCharityStatus (int memberId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#scheduleReboot}.
     */
    void scheduleReboot (int minutes, String message, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getPeerNodeNames}.
     */
    void getPeerNodeNames (AsyncCallback<Set<String>> callback);

    /**
     * The asynchronous version of {@link AdminService#restartPanopticon}.
     */
    void restartPanopticon (Set<String> nodeNames, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getBroadcastHistory}.
     */
    void getBroadcastHistory (int offset, int count, boolean needCount,
        AsyncCallback<BroadcastHistoryResult> callback);

    /**
     * The asynchronous version of {@link AdminService#summarizeEntries}.
     */
    void summarizeEntries (AsyncCallback<List<EntrySummary>> callback);

    /**
     * The asynchronous version of {@link AdminService#loadFacebookTemplates}.
     */
    void loadFacebookTemplates (AsyncCallback<List<FacebookTemplate>> callback);
    
    /**
     * The asynchronous version of {@link AdminService#updateFacebookTemplates}.
     */
    void updateFacebookTemplates (Set<FacebookTemplate> changed, Set<FacebookTemplate> removed,
        AsyncCallback<Void> callback);
}
