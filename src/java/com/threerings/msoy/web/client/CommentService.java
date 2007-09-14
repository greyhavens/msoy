//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Service methods for reading and posting comments on various Whirled entities (items, profiles,
 * rooms, etc.).
 */
public interface CommentService extends RemoteService
{
    /**
     * Loads recent comments made about the specified entity.
     *
     * @gwt.typeArgs <com.threerings.msoy.fora.data.Comment>
     */
    public List loadComments (int entityType, int entityId, int offset, int count)
        throws ServiceException;

    /**
     * Posts a comment on the specified entity.
     *
     * @return the comment that was posted, throws an exception on failure.
     */
    public Comment postComment (WebIdent ident, int entityType, int entityId, String text)
        throws ServiceException;

    /**
     * Deletes the specified comment from the specified entity. The caller must be the owner of the
     * comment or an admin.
     *
     * @return true if the comment was deleted, throws an exception on failure.
     */
    public boolean deleteComment (WebIdent ident, int entityType, int entityId, long when)
        throws ServiceException;
}
