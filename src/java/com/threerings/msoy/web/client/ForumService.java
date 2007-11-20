//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

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
    /**
     * Loads the specified range of threads for the specified group.
     *
     * @gwt.typeArgs <com.threerings.msoy.fora.data.ForumThread>
     */
    public List loadThreads (WebIdent ident, int groupId, int offset, int count)
        throws ServiceException;

    /**
     * Loads the specified range of messages for the specified thread.
     *
     * @gwt.typeArgs <com.threerings.msoy.fora.data.ForumMessage>
     */
    public List loadMessages (WebIdent ident, int threadId, int offset, int count)
        throws ServiceException;

    /**
     * Creates a new thread for the specified group.
     */
    public ForumThread createThread (WebIdent ident, int groupId, String subject, String message)
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
