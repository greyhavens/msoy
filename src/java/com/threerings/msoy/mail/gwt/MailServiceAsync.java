//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.group.gwt.GroupService;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface MailServiceAsync
{
    /**
     * The asynchronous version of {@link MailService#loadConversations}
     */
    public void loadConversations (WebIdent ident, int offset, int count, boolean needCount,
                                   AsyncCallback<MailService.ConvosResult> callback);

    /**
     * The asynchronous version of {@link MailService#loadConversation}
     */
    public void loadConversation (WebIdent ident, int convoId,
                                  AsyncCallback<MailService.ConvoResult> callback);

    /**
     * The asynchronous version of {@link MailService#startConversation}
     */
    public void startConversation (WebIdent ident, int recipientId, String subject, String body,
                                   MailPayload attachment, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MailService#continueConversation}
     */
    public void continueConversation (WebIdent ident, int convoId, String text,
                                      MailPayload attachment, AsyncCallback<ConvMessage> callback);

    /**
     * The asynchronous version of {@link MailService#deleteConversation}
     */
    void deleteConversation (WebIdent ident, int convoId, AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link MailService#updatePayload}
     */
    public void updatePayload (WebIdent ident, int convoId, long sent, MailPayload obj,
                               AsyncCallback<Void> callback);
}
