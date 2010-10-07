//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;
import com.threerings.gwt.util.PagedResult;

/**
 * Defines forum related services available to the GWT client.
 */
@RemoteServiceRelativePath(ForumService.REL_PATH)
public interface ForumService extends RemoteService
{
    /** Maximum length allowed for a message complaint. Note: this must be the same as the maximum
     * length of {@link com.threerings.underwire.server.persist.EventRecord#subject}, but we cannot
     * easily share code here. */
    public static final int MAX_COMPLAINT_LENGTH = 255;

    /** Provides results for {@link #loadThreads}. */
    public static class ThreadResult extends PagedResult<ForumThread>
    {
        /** Whether or not the caller can start a thread in this group. */
        public boolean canStartThread;

        /** True if we're manager of the group from which these threads came. */
        public boolean isManager;

        /** True if this is the global announcements forum, false otherwise. */
        public boolean isAnnounce;
    }

    /** Provides results for {@link #loadMessages}. */
    public static class MessageResult implements IsSerializable
    {
        /** The thread for which messages were loaded, only provided for first request. */
        public ForumThread thread;

        /** Whether or not the caller can post a reply message to this thread. */
        public boolean canPostReply;

        /** Returns true if we're manager of the group from which this thread came. */
        public boolean isManager;

        /** The range of messages that were requested. */
        public List<ForumMessage> messages;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/forumsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + ForumService.ENTRY_POINT;

    /**
     * Loads up to <code>maximum</code> threads from groups of which the caller is a member for
     * which there are messages not yet read by same. The threads are sorted from most to least
     * recently active.
     */
    List<ForumThread> loadUnreadThreads (int maximum)
        throws ServiceException;

    /**
     * Loads up to <code>maximum</code> threads which contain unread posts by the caller's friends.
     */
    List<ForumThread> loadUnreadFriendThreads (int maximum)
        throws ServiceException;

    /**
     * Loads the specified range of threads for the specified group.
     */
    ThreadResult loadThreads (int groupId, int offset, int count, boolean needTotalCount)
        throws ServiceException;

    /**
     * Searches the subjects and messages in all threads in the specified group.
     */
    List<ForumThread> findThreads (int groupId, String search, int limit)
        throws ServiceException;

    /**
     * Searches the subjects and messages in all threads in groups to which the caller belongs
     */
    List<ForumThread> findMyThreads (String search, int limit)
        throws ServiceException;

    /**
     * Loads the specified range of messages for the specified thread.
     */
    MessageResult loadMessages (int threadId, int lastReadPostId, int offset, int count,
                                boolean needTotalCount)
        throws ServiceException;

    /**
     * Searches the messages in a particular thread.
     */
    List<ForumMessage> findMessages (int threadId, String search, int limit)
        throws ServiceException;

    /**
     * Creates a new thread for the specified group.
     */
    ForumThread createThread (int groupId, int flags, boolean spam, String subject, String message)
        throws ServiceException;

    /**
     * Updates the specified thread's flags and subject.
     */
    void updateThread (int threadId, int flags, String subject)
        throws ServiceException;

    /**
     * Marks the specified thread as ignored by this player.
     */
    void ignoreThread (int threadId)
        throws ServiceException;

    /**
     * Posts a message to the specified thread.
     */
    ForumMessage postMessage (int threadId, int inReplyTo, int inReplyToMemberId, String message)
        throws ServiceException;

    /**
     * Edits a previously posted message.
     */
    ForumMessage editMessage (int messageId, String message)
        throws ServiceException;

    /**
     * Deletes the specified message.
     */
    void deleteMessage (int messageId)
        throws ServiceException;

    /**
     * Complains about the specified message.
     */
    void complainMessage (String complaint, int messageId)
        throws ServiceException;

    /**
     * Formats the supplied subject and message into an email and delivers it to the caller. This
     * allows admins and support to preview their in-progress newsletter posts before they pull the
     * trigger and spam the entire userbase with them.
     *
     * @param includeProbeList if true, the Return Path probe emails will also have the mail
     * delivered to them.
     */
    void sendPreviewEmail (String subject, String message, boolean includeProbeList)
        throws ServiceException;
}
