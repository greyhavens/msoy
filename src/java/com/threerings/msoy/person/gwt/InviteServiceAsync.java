//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.gwt.EmailContact;

/**
 * The asynchronous (client-side) version of {@link InviteService}.
 */
public interface InviteServiceAsync
{
    /**
     * The asynchronous version of {@link InviteService#getWebMailAddresses}.
     */
    void getWebMailAddresses (String email, String password,
                              AsyncCallback<List<EmailContact>> callback);

    /**
     * The asynchronous version of {@link InviteService#getInvitationsStatus}.
     */
    void getInvitationsStatus (AsyncCallback<MemberInvites> callback);

    /**
     * The asynchronous version of {@link InviteService#sendInvites}.
     */
    void sendInvites (List<EmailContact> addresses, String fromName, String customMessage,
                      boolean anonymous, AsyncCallback<InvitationResults> callback);

    /**
     * The asynchronous version of {@link InviteService#removeInvitation}.
     */
    void removeInvitation (String inviteId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link InviteService#getHomeSceneId}.
     */
    void getHomeSceneId (AsyncCallback<Integer> callback);
}
