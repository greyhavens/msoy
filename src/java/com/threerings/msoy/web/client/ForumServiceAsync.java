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
    public void loadThreads (WebIdent ident, int groupId, int offset, int count, AsyncCallback cb);

    /**
     * The asynchronous version of {@link ForumService#loadMessages}.
     */
    public void loadMessages (WebIdent ident, int threadId, int offset, int count, AsyncCallback cb);
}
