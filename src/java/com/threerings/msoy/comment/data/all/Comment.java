//
// $Id$

package com.threerings.msoy.comment.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Contains runtime data for a comment made by a member on something.
 */
public class Comment
    implements IsSerializable
{

    /** The maximum length of comment text. */
    public static final int MAX_TEXT_LENGTH = 1024;

    /** The number of comments displayed per page. */
    public static final int COMMENTS_PER_PAGE = 12;

    /** The rating below which comments are hidden in the interface. */
    public static final int RATED_HIDDEN = -2;

    /** The rating above which comments are emphasized in the interface. */
    public static final int RATED_EMPHASIZED = 5;

    /**
     * Returns true if the specified member can delete a comment.
     */
    public static boolean canDelete (CommentType type, int entityId, int commentorId, int memberId)
    {
        return (memberId == commentorId) ||
            (type.forProfileWall() && entityId == memberId);
    }

    /** The member that made this comment. */
    public MemberName commentor;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = MemberCard.DEFAULT_PHOTO;

    /** The time at which this comment was posted. */
    public long posted;

    /** The posted time of the original comment this comment is replying to, or 0 if not a reply. */
    public long replyTo;

    /** The absolute rating of this comment. */
    public int currentRating;

    /** The total number of times this comment has been rated. */
    public int totalRatings;

    /** The text of this comment. */
    public String text;

    public boolean isReply ()
    {
        return replyTo != 0;
    }
}
