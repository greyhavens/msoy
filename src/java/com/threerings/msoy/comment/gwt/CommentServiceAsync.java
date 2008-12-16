//
// $Id$

package com.threerings.msoy.comment.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link CommentService}.
 */
public interface CommentServiceAsync
{
    /**
     * The asynchronous version of {@link CommentService#loadComments}.
     */
    void loadComments (int entityType, int entityId, int offset, int count, boolean needCount,
                       AsyncCallback<CommentService.CommentResult> callback);

    /**
     * The asynchronous version of {@link CommentService#postComment}.
     */
    void postComment (int entityType, int entityId, String text, AsyncCallback<Comment> callback);

    /**
     * The asynchronous version of {@link CommentService#rateComment}.
     */
    void rateComment (int entityType, int entityId, long posted, boolean rating,
                      AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link CommentService#deleteComment}.
     */
    void deleteComment (int entityType, int entityId, long when, AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link CommentService#complainComment}.
     */
    void complainComment (String subject, int entityType, int entityId, long when,
                          AsyncCallback<Void> callback);
}
