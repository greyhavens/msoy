//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

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
}
