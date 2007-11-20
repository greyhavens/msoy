//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link ForumService}.
 */
public interface ForumServiceAsync
{
    /**
     * The asynchronous version of {@link ForumService#loadThreads}.
     */
    public void loadThreads (WebIdent ident, int groupId, int offset, int count,
                             AsyncCallback callback);

    /**
     * The asynchronous version of {@link ForumService#loadMessages}.
     */
    public void loadMessages (WebIdent ident, int threadId, int offset, int count,
                              AsyncCallback callback);

    /**
     * The asynchronous version of {@link ForumService#createThread}.
     */
    public void createThread (WebIdent ident, int groupId, String subject, String message,
                              AsyncCallback callback);

    /**
     * The asynchronous version of {@link ForumService#postMessage}.
     */
    public void postMessage (WebIdent ident, int threadId, int inReplyTo, String message,
                             AsyncCallback callback);

    /**
     * The asynchronous version of {@link ForumService#editMessage}.
     */
    public void editMessage (WebIdent ident, int messageId, String message, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ForumService#deleteMessage}.
     */
    public void deleteMessage (WebIdent ident, int messageId, AsyncCallback callback);
}
