//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.group.gwt.GroupService;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface MailServiceAsync
{
    /**
     * The asynchronous version of {@link MailService#loadConversations}
     */
    public void loadConversations (int offset, int count, boolean needCount,
                                   AsyncCallback<MailService.ConvosResult> callback);

    /**
     * The asynchronous version of {@link MailService#loadConversation}
     */
    public void loadConversation (int convoId, AsyncCallback<MailService.ConvoResult> callback);

    /**
     * The asynchronous version of {@link MailService#startConversation}
     */
    public void startConversation (int recipientId, String subject, String body,
                                   MailPayload attachment, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MailService#continueConversation}
     */
    public void continueConversation (int convoId, String text, MailPayload attachment,
                                      AsyncCallback<ConvMessage> callback);

    /**
     * The asynchronous version of {@link MailService#deleteConversation}
     */
    void deleteConversation (int convoId, boolean ignoreUnread, AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link MailService#updatePayload}
     */
    public void updatePayload (int convoId, long sent, MailPayload obj,
                               AsyncCallback<Void> callback);
}
