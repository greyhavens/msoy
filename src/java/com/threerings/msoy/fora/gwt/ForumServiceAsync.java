//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link ForumService}.
 */
public interface ForumServiceAsync
{
    /**
     * The asynchronous version of {@link ForumService#loadUnreadThreads}.
     */
    void loadUnreadThreads (int maximum, AsyncCallback<ForumService.ThreadResult> callback);

    /**
     * The asynchronous version of {@link ForumService#loadThreads}.
     */
    void loadThreads (int groupId, int offset, int count, boolean needTotalCount,
                      AsyncCallback<ForumService.ThreadResult> callback);

    /**
     * The asynchronous version of {@link ForumService#findThreads}.
     */
    void findThreads (int groupId, String query, int limit,
                      AsyncCallback<List<ForumThread>> callback);

    /**
     * The asynchronous version of {@link ForumService#loadMessages}.
     */
    void loadMessages (int threadId, int lastReadPostId, int offset, int count,
                       boolean needTotalCount, AsyncCallback<ForumService.MessageResult> callback);

    /**
     * The asynchronous version of {@link ForumService#findMessages}.
     */
    void findMessages (int threadId, String search, int limit,
                       AsyncCallback<List<ForumMessage>> callback);

    /**
     * The asynchronous version of {@link ForumService#createThread}.
     */
    void createThread (int groupId, int flags, String subject, String message,
                       AsyncCallback<ForumThread> callback);

    /**
     * The asynchronous version of {@link ForumService#updateThreadFlags}.
     */
    void updateThreadFlags (int threadId, int flags, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#ignoreThread}.
     */
    void ignoreThread (int threadId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#postMessage}.
     */
    void postMessage (int threadId, int inReplyTo, String message,
                      AsyncCallback<ForumMessage> callback);

    /**
     * The asynchronous version of {@link ForumService#editMessage}.
     */
    void editMessage (int messageId, String message, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#deleteMessage}.
     */
    void deleteMessage (int messageId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ForumService#complainMessage}.
     */
    void complainMessage (String complaint, int messageId, AsyncCallback<Void> callback);
}
