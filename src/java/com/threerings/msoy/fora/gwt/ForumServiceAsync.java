//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link ForumService}.
 */
public interface ForumServiceAsync
{
    /**
     * The async version of {@link ForumService#loadUnreadThreads}.
     */
    void loadUnreadThreads (int offset, int count, AsyncCallback<List<ForumThread>> callback);

    /**
     * The async version of {@link ForumService#loadUnreadFriendThreads}.
     */
    void loadUnreadFriendThreads (int offset, int count, AsyncCallback<List<ForumThread>> callback);

    /**
     * The async version of {@link ForumService#loadThreads}.
     */
    void loadThreads (int groupId, int offset, int count, AsyncCallback<ForumService.ThreadResult> callback);

    /**
     * The async version of {@link ForumService#findThreads}.
     */
    void findThreads (int groupId, String search, int offset, int count, AsyncCallback<List<ForumThread>> callback);

    /**
     * The async version of {@link ForumService#findMyThreads}.
     */
    void findMyThreads (String search, int offset, int count, AsyncCallback<List<ForumThread>> callback);

    /**
     * The async version of {@link ForumService#loadMessages}.
     */
    void loadMessages (int threadId, int lastReadPostId, int offset, int count, boolean needCount, AsyncCallback<ForumService.MessageResult> callback);

    /**
     * The async version of {@link ForumService#findMessages}.
     */
    void findMessages (int threadId, String search, int offset, int count, AsyncCallback<List<ForumMessage>> callback);

    /**
     * The async version of {@link ForumService#createThread}.
     */
    void createThread (int groupId, int flags, boolean spam, String subject, String message, AsyncCallback<ForumThread> callback);

    /**
     * The async version of {@link ForumService#updateThread}.
     */
    void updateThread (int threadId, int flags, String subject, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ForumService#ignoreThread}.
     */
    void ignoreThread (int threadId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ForumService#postMessage}.
     */
    void postMessage (int threadId, int inReplyTo, int inReplyToMemberId, String message, AsyncCallback<ForumMessage> callback);

    /**
     * The async version of {@link ForumService#editMessage}.
     */
    void editMessage (int messageId, String message, AsyncCallback<ForumMessage> callback);

    /**
     * The async version of {@link ForumService#deleteMessage}.
     */
    void deleteMessage (int messageId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ForumService#complainMessage}.
     */
    void complainMessage (String complaint, int messageId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ForumService#sendPreviewEmail}.
     */
    void sendPreviewEmail (String subject, String message, boolean includeProbeList, AsyncCallback<Void> callback);
}
