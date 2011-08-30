//
// $Id$

package com.threerings.msoy.mail.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link MailService}.
 */
public interface MailServiceAsync
{
    /**
     * The async version of {@link MailService#startConversation}.
     */
    void startConversation (int recipientId, String subject, String body, MailPayload attachment, AsyncCallback<Void> callback);

    /**
     * The async version of {@link MailService#continueConversation}.
     */
    void continueConversation (int convoId, String text, MailPayload attachment, AsyncCallback<ConvMessage> callback);

    /**
     * The async version of {@link MailService#deleteConversation}.
     */
    void deleteConversation (int convoId, boolean ignoreUnread, AsyncCallback<Boolean> callback);

    /**
     * The async version of {@link MailService#deleteConversations}.
     */
    void deleteConversations (List<Integer> convoIds, AsyncCallback<Void> callback);

    /**
     * The async version of {@link MailService#updatePayload}.
     */
    void updatePayload (int convoId, long sent, MailPayload payload, AsyncCallback<Void> callback);

    /**
     * The async version of {@link MailService#complainConversation}.
     */
    void complainConversation (int convoId, String reason, AsyncCallback<Void> callback);

    /**
     * The async version of {@link MailService#loadConversations}.
     */
    void loadConversations (int offset, int count, boolean needCount, AsyncCallback<MailService.ConvosResult> callback);

    /**
     * The async version of {@link MailService#loadConversation}.
     */
    void loadConversation (int convoId, AsyncCallback<MailService.ConvoResult> callback);
}
