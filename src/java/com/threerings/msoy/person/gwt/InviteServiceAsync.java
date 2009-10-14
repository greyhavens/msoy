//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;
import java.util.Set;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Provides the asynchronous version of {@link InviteService}.
 */
public interface InviteServiceAsync
{
    /**
     * The async version of {@link InviteService#getWebMailAddresses}.
     */
    void getWebMailAddresses (String email, String password, AsyncCallback<List<EmailContact>> callback);

    /**
     * The async version of {@link InviteService#sendInvites}.
     */
    void sendInvites (List<EmailContact> addresses, String fromName, String subject, String customMessage, boolean anonymous, AsyncCallback<InvitationResults> callback);

    /**
     * The async version of {@link InviteService#sendGameInvites}.
     */
    void sendGameInvites (List<EmailContact> addresses, int gameId, String from, String subject, String body, AsyncCallback<InvitationResults> callback);

    /**
     * The async version of {@link InviteService#sendWhirledMailGameInvites}.
     */
    void sendWhirledMailGameInvites (Set<Integer> recipientIds, int gameId, String subject, String body, String args, AsyncCallback<Void> callback);

    /**
     * The async version of {@link InviteService#getHomeSceneId}.
     */
    void getHomeSceneId (AsyncCallback<Integer> callback);

    /**
     * The async version of {@link InviteService#getFriends}.
     */
    void getFriends (int gameId, int count, AsyncCallback<List<MemberCard>> callback);
}
