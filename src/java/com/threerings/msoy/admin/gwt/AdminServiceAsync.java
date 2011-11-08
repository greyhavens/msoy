//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.List;
import java.util.Set;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Provides the asynchronous version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The async version of {@link AdminService#getMemberInfo}.
     */
    void getMemberInfo (int memberId, int affiliateOfCount, AsyncCallback<MemberAdminInfo> callback);

    /**
     * The async version of {@link AdminService#getAffiliates}.
     */
    void getAffiliates (int memberId, int offset, int count, AsyncCallback<List<MemberName>> callback);

    /**
     * The async version of {@link AdminService#setRole}.
     */
    void setRole (int memberId, WebCreds.Role role, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#setDisplayName}.
     */
    void setDisplayName (int memberId, String name, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#setPermaName}.
     */
    void setPermaName (int memberId, String name, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#setValidated}.
     */
    void setValidated (int memberId, boolean value, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#getABTests}.
     */
    void getABTests (AsyncCallback<List<ABTest>> callback);

    /**
     * The async version of {@link AdminService#getABTestSummary}.
     */
    void getABTestSummary (int testId, AsyncCallback<ABTestSummary> callback);

    /**
     * The async version of {@link AdminService#getItemFlags}.
     */
    void getItemFlags (int from, int count, boolean needCount, AsyncCallback<AdminService.ItemFlagsResult> callback);

    /**
     * The async version of {@link AdminService#getItemTransactions}.
     */
    void getItemTransactions (ItemIdent iident, int from, int count, AsyncCallback<AdminService.ItemTransactionResult> callback);

    /**
     * The async version of {@link AdminService#deleteItemAdmin}.
     */
    void deleteItemAdmin (ItemIdent item, String subject, String body, AsyncCallback<AdminService.ItemDeletionResult> callback);

    /**
     * The async version of {@link AdminService#nukeMedia}.
     */
    void nukeMedia (byte[] hash, byte type, String note, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#getBureauLauncherInfo}.
     */
    void getBureauLauncherInfo (AsyncCallback<BureauLauncherInfo[]> callback);

    /**
     * The async version of {@link AdminService#loadPromotions}.
     */
    void loadPromotions (AsyncCallback<List<Promotion>> callback);

    /**
     * The async version of {@link AdminService#addPromotion}.
     */
    void addPromotion (Promotion promo, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#updatePromotion}.
     */
    void updatePromotion (Promotion promo, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#deletePromotion}.
     */
    void deletePromotion (String promoId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#loadContests}.
     */
    void loadContests (AsyncCallback<List<Contest>> callback);

    /**
     * The async version of {@link AdminService#addContest}.
     */
    void addContest (Contest contest, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#updateContest}.
     */
    void updateContest (Contest contest, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#deleteContest}.
     */
    void deleteContest (String contestId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#getStatsModel}.
     */
    void getStatsModel (StatsModel.Type type, AsyncCallback<StatsModel> callback);

    /**
     * The async version of {@link AdminService#setCharityInfo}.
     */
    void setCharityInfo (CharityInfo charityInfo, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#removeCharityStatus}.
     */
    void removeCharityStatus (int memberId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#scheduleReboot}.
     */
    void scheduleReboot (int minutes, String message, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#getPeerNodeNames}.
     */
    void getPeerNodeNames (AsyncCallback<Set<String>> callback);

    /**
     * The async version of {@link AdminService#restartPanopticon}.
     */
    void restartPanopticon (Set<String> nodeNames, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#getBroadcastHistory}.
     */
    void getBroadcastHistory (int offset, int count, boolean needCount, AsyncCallback<AdminService.BroadcastHistoryResult> callback);
}
