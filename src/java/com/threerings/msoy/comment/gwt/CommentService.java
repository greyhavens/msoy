//
// $Id$

package com.threerings.msoy.comment.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Service methods for reading and posting comments on various Whirled entities (items, profiles,
 * rooms, etc.).
 */
public interface CommentService extends RemoteService
{
    /** Provides results for {@link #loadComments}. */
    public static class CommentResult implements IsSerializable
    {
        /** The total count of comments. */
        public int commentCount;

        /** The range of comments that were requested. */
        public List<Comment> comments;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/commentsvc";

    /**
     * Loads recent comments made about the specified entity.
     */
    public CommentResult loadComments (int entityType, int entityId, int offset, int count,
                                       boolean needCount)
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

    /**
     * Complains the specified comment from the specified entity.
     */
    public void complainComment (
            WebIdent ident, String subject, int entityType, int entityId, long when)
        throws ServiceException;
}
