//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Defines forum related services available to the GWT client.
 */
public interface ForumService extends RemoteService
{
    /** Provides results for {@link #loadThreads}. */
    public static class ThreadResult implements IsSerializable
    {
        /** The total count of threads. */
        public int threadCount;

        /** Whether or not the caller can start a thread in this group. */
        public boolean canStartThread;

        /** True if we're manager of the group from which these threads came. */
        public boolean isManager;

        /** True if this is the global announcements forum, false otherwise. */
        public boolean isAnnounce;

        /** The range of threads that were requested. */
        public List<ForumThread> threads;
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

    /**
     * Loads up to <code>maximum</code> threads from groups of which the caller is a member for
     * which there are messages not yet read by same. The threads are sorted from most to least
     * recently active.
     */
    ThreadResult loadUnreadThreads (int maximum)
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
     * Updates the specified thread's flags.
     */
    void updateThreadFlags (int threadId, int flags)
        throws ServiceException;

    /**
     * Marks the specified thread as ignored by this player.
     */
    void ignoreThread (int threadId)
        throws ServiceException;

    /**
     * Posts a message to the specified thread.
     */
    ForumMessage postMessage (int threadId, int inReplyTo, String message)
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
     */
    void sendPreviewEmail (String subject, String message)
        throws ServiceException;
}
