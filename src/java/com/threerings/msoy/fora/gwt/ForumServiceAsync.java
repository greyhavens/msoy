//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link ForumService}.
 */
public interface ForumServiceAsync
{
    /**
     * The asynchronous version of {@link ForumService#loadUnreadThreads}.
     */
    public void loadUnreadThreads (WebIdent ident, int maximum,
                                   AsyncCallback<ForumService.ThreadResult> callback);

    /**
     * The asynchronous version of {@link ForumService#loadThreads}.
     */
    public void loadThreads (WebIdent ident, int groupId, int offset, int count,
                             boolean needTotalCount,
                             AsyncCallback<ForumService.ThreadResult> callback);

    /**
     * The asynchronous version of {@link ForumService#findThreads}.
     */
    public void findThreads (WebIdent ident, int groupId, String query, int limit,
                             AsyncCallback<List<ForumThread>> callback);

    /**
     * The asynchronous version of {@link ForumService#loadMessages}.
     */
    public void loadMessages (WebIdent ident, int threadId, int lastReadPostId, int offset,
                              int count, boolean needTotalCount,
                              AsyncCallback<ForumService.MessageResult> callback);

    /**
     * The asynchronous version of {@link ForumService#findMessages}.
     */
    public void findMessages (WebIdent ident, int threadId, String search, int limit,
                              AsyncCallback<List<ForumMessage>> callback);

    /**
     * The asynchronous version of {@link ForumService#createThread}.
     */
    public void createThread (WebIdent ident, int groupId, int flags, String subject,
                              String message, AsyncCallback<ForumThread> callback);

    /**
     * The asynchronous version of {@link ForumService#updateThreadFlags}.
     */
    public void updateThreadFlags (WebIdent ident, int threadId, int flags,
                                   AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#ignoreThread}.
     */
    void ignoreThread (WebIdent ident, int threadId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#postMessage}.
     */
    public void postMessage (WebIdent ident, int threadId, int inReplyTo, String message,
                             AsyncCallback<ForumMessage> callback);

    /**
     * The asynchronous version of {@link ForumService#editMessage}.
     */
    public void editMessage (WebIdent ident, int messageId, String message,
                             AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#deleteMessage}.
     */
    void deleteMessage (WebIdent ident, int messageId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#complainMessage}.
     */
    public void complainMessage (WebIdent ident, String complaint, int messageId,
                                 AsyncCallback<Void> callback);
}
