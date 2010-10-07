//
// $Id$

package com.threerings.msoy.comment.gwt;

import java.util.Collection;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.comment.gwt.Comment.CommentType;

/**
 * Service methods for reading and posting comments on various Whirled entities (items, profiles,
 * rooms, etc.).
 */
@RemoteServiceRelativePath(CommentService.REL_PATH)
public interface CommentService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/commentsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + CommentService.ENTRY_POINT;

    /** Maximum length allowed for a comment complaint. Note: this must be the same as the maximum
     * length of {@link com.threerings.underwire.server.persist.EventRecord#subject}, but we cannot
     * easily share code here. */
    public static final int MAX_COMPLAINT_LENGTH = 255;

    /**
     * Loads recent comments made about the specified entity.
     */
    PagedResult<Comment> loadComments (
        CommentType entityType, int entityId, int offset, int count, boolean needCount)
        throws ServiceException;

    /**
     * Posts a comment on the specified entity.
     *
     * @return the comment that was posted, throws an exception on failure.
     */
    Comment postComment (CommentType entityType, int entityId, String text)
        throws ServiceException;

    /**
     * Rate the given comment up (true) or down (false).
     *
     * @return how much the item's rating changed: -2, -1, 0, 1 or 2
     */
    int rateComment (CommentType entityType, int entityId, long posted, boolean rating)
        throws ServiceException;

    /**
     * Deletes the specified comment from the specified entity. The caller must be the owner of the
     * comment or an admin.
     *
     * @return true if the comment was deleted, throws an exception on failure.
     */
    int deleteComments (CommentType entityType, int entityId, Collection<Long> when)
        throws ServiceException;

    /**
     * Complains the specified comment from the specified entity.
     */
    void complainComment (String subject, CommentType entityType, int entityId, long when)
        throws ServiceException;
}
