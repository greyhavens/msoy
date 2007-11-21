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

        /** The range of threads that were requested.
         * @gwt.typeArgs <com.threerings.msoy.fora.data.ForumThread> */
        public List threads;
    }

    /** Provides results for {@link #loadMessages}. */
    public static class MessageResult implements IsSerializable
    {
        /** The total count of messages in the specified thread. */
        public int messageCount;

        /** Whether or not the caller can post a message to this thread. */
        public boolean canPostMessage;

        /** The range of messages that were requested.
         * @gwt.typeArgs <com.threerings.msoy.fora.data.ForumMessage> */
        public List messages;
    }

    /**
     * Loads the specified range of threads for the specified group.
     */
    public ThreadResult loadThreads (WebIdent ident, int groupId, int offset, int count,
                                     boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads the specified range of messages for the specified thread.
     */
    public MessageResult loadMessages (WebIdent ident, int threadId, int offset, int count,
                                       boolean needTotalCount)
        throws ServiceException;

    /**
     * Creates a new thread for the specified group.
     */
    public ForumThread createThread (WebIdent ident, int groupId, int flags,
                                     String subject, String message)
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
}
