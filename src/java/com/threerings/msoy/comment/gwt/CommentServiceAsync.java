//
// $Id$

package com.threerings.msoy.comment.gwt;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.comment.data.all.CommentType;

/**
 * Provides the asynchronous version of {@link CommentService}.
 */
public interface CommentServiceAsync
{
    /**
     * The async version of {@link CommentService#loadComments}.
     */
    void loadComments (CommentType entityType, int entityId, int offset, int count, boolean needCount, AsyncCallback<PagedResult<Comment>> callback);

    /**
     * The async version of {@link CommentService#postComment}.
     */
    void postComment (CommentType entityType, int entityId, String text, AsyncCallback<Comment> callback);

    /**
     * The async version of {@link CommentService#rateComment}.
     */
    void rateComment (CommentType entityType, int entityId, long posted, boolean rating, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link CommentService#deleteComments}.
     */
    void deleteComments (CommentType entityType, int entityId, Collection<Long> when, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link CommentService#complainComment}.
     */
    void complainComment (String subject, CommentType entityType, int entityId, long when, AsyncCallback<Void> callback);
}
