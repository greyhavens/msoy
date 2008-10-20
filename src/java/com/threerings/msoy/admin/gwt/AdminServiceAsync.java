//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * The asynchronous (client-side) version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The asynchronous version of {@link AdminService#getAffiliateMappings}.
     */
    void getAffiliateMappings (
        int start, int count, boolean needTotal,
        AsyncCallback<PagedResult<AffiliateMapping>> callback);

    /**
     * The asynchronous version of {@link AdminService#mapAffiliate}.
     */
    void mapAffiliate (String affiliate, int memberId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#grantInvitations}.
     */
    void grantInvitations (int numberInvitations, Date activeSince, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#grantInvitations}.
     */
    void grantInvitations (int numberInvitations, int memberId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getMemberInfo}.
     */
    void getMemberInfo (int memberId, AsyncCallback<MemberAdminInfo> callback);

    /**
     * The asynchronous version of {@link AdminService#getPlayerList}.
     */
    void getPlayerList (int inviterId, AsyncCallback<MemberInviteResult> callback);

    /**
     * The asynchronous version of {@link AdminService#setRole}.
     */
    void setRole (int memberId, WebCreds.Role role, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getABTests}.
     */
    void getABTests (AsyncCallback<List<ABTest>> callback);

    /**
     * The asynchronous version of {@link AdminService#createTest}.
     */
    void createTest (ABTest test, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#updateTest}.
     */
    void updateTest (ABTest test, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link AdminService#getFlaggedItems}.
     */
    void getFlaggedItems (int count, AsyncCallback<List<ItemDetail>> callback);

    /**
     * The asynchronous version of {@link AdminService#deleteItemAdmin}.
     */
    void deleteItemAdmin (ItemIdent item, String subject, String body,
                          AsyncCallback<Integer> callback);
}
