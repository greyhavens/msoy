//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.person.data.MailPayload;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface MailServiceAsync
{
    /**
     * The asynchronous version of {@link MailService#loadConversations}
     */
    public void loadConversations (WebIdent ident, int offset, int count, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#loadConversation}
     */
    public void loadConversation (WebIdent ident, int convoId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#startConversation}
     */
    public void startConversation (WebIdent ident, int recipientId, String subject, String text,
                                   MailPayload attachment, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#continueConversation}
     */
    public void continueConversation (WebIdent ident, int convoId, String text,
                                      MailPayload attachment, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getFolder}
     */
    public void getFolder (WebIdent ident, int folderId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getFolders}
     */
    public void getFolders (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getHeaders}
     */
    public void getHeaders (WebIdent ident, int folderId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getConversation}
     */
    public void getConversation (WebIdent ident, int folderId, int convId,
                                 AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getMessage}
     */
    public void getMessage (WebIdent ident, int folderId, int messageId,
                            AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#deliverMessage}
     */
    public void deliverMessage (WebIdent ident, int recipientId, String subject, String text,
                                MailPayload object, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#updatePayload}
     */
    public void updatePayload (WebIdent ident, int folderId, int messageId,
                               MailPayload obj, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#deleteMessages}
     */
    public void deleteMessages (WebIdent ident, int folderId, int[] msgIdArr,
                                AsyncCallback callback);
}
