//
// $Id$

package com.threerings.msoy.comment.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Contains runtime data for a comment made by a member on something.
 */
public class Comment
    implements IsSerializable
{
    /** The minimum entity type code reserved for items. */
    public static final int TYPE_ITEM_MIN = 1;

    /** The maximum entity type code reserved for items. */
    public static final int TYPE_ITEM_MAX = 63;

    /** An entity type code indicating a comment on a room. */
    public static final int TYPE_ROOM = 64;

    /** An entity type code indicating a comment on a member's profile. */
    public static final int TYPE_PROFILE_WALL = 65;

    /** The maximum length of comment text. */
    public static final int MAX_TEXT_LENGTH = 1024;

    /** The number of comments displayed per page. */
    public static final int COMMENTS_PER_PAGE = 5;

    /** A value for {@link #myRating} that means we have not yet rated this item. */
    public static final int RATED_NONE = 0;

    /** A value for {@link #myRating} that means we have rated this item positively. */
    public static final int RATED_UP = 1;

    /** A value for {@link #myRating} that means we have rated this item negatively. */
    public static final int RATED_DOWN = 2;

    /**
     * Returns true if the specified member can delete a comment.
     */
    public static boolean canDelete (int type, int entityId, int commentorId, int memberId)
    {
        return (memberId == commentorId) ||
            (type == Comment.TYPE_PROFILE_WALL && entityId == memberId);
    }

    /** The member that made this comment. */
    public MemberName commentor;

    /** The member's profile photo (or the default). */
    public MediaDesc photo = MemberCard.DEFAULT_PHOTO;

    /** The time at which this comment was posted. */
    public long posted;

    /** The absolute rating of this comment. */
    public int currentRating;

    /** The total number of times this comment has been rated. */
    public int totalRatings;

    /**
     * The authenticated user's rating of this comment. See {@link #RATED_NONE},
     * {@link #RATED_UP}, {@link #RATED_DOWN}.
     */
    public int myRating;

    /** The text of this comment. */
    public String text;
}
