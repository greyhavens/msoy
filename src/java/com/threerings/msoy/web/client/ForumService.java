//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

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

        /** Returns true if we're manager of the group from which these threads came. */
        public boolean isManager;

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
    public ThreadResult loadUnreadThreads (WebIdent ident, int maximum)
        throws ServiceException;

    /**
     * Loads the specified range of threads for the specified group.
     */
    public ThreadResult loadThreads (WebIdent ident, int groupId, int offset, int count,
                                     boolean needTotalCount)
        throws ServiceException;

    /**
     * Searches the subjects and messages in all threads in the specified group.
     */
    public List<ForumThread> findThreads (WebIdent ident, int groupId, String search, int limit)
        throws ServiceException;

    /**
     * Loads the specified range of messages for the specified thread.
     */
    public MessageResult loadMessages (WebIdent ident, int threadId, int lastReadPostId,
                                       int offset, int count, boolean needTotalCount)
        throws ServiceException;

    /**
     * Searches the messages in a particular thread.
     */
    public List<ForumMessage> findMessages (WebIdent ident, int threadId, String search, int limit)
        throws ServiceException;

    /**
     * Creates a new thread for the specified group.
     */
    public ForumThread createThread (WebIdent ident, int groupId, int flags,
                                     String subject, String message)
        throws ServiceException;

    /**
     * Updates the specified thread's flags.
     */
    public void updateThreadFlags (WebIdent ident, int threadId, int flags)
        throws ServiceException;

    /**
     * Marks the specified thread as ignored by this player.
     */
    public void ignoreThread (WebIdent ident, int threadId)
        throws ServiceException;

    /**
     * Posts a message to the specified thread.
     */
    public ForumMessage postMessage (WebIdent ident, int threadId, int inReplyTo, String message)
        throws ServiceException;

    /**
     * Edits a previously posted message.
     */
    public void editMessage (WebIdent ident, int messageId, String message)
        throws ServiceException;

    /**
     * Deletes the specified message.
     */
    public void deleteMessage (WebIdent ident, int messageId)
        throws ServiceException;

    /**
     * Complains about the specified message.
     */
    public void complainMessage (WebIdent ident, String complaint, int messageId)
        throws ServiceException;
}
