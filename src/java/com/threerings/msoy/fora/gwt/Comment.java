//
// $Id$

package com.threerings.msoy.fora.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

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

    /**
     * Returns true if this is a valid comment entity type, false if not.
     */
    public static boolean isValidType (int entityType)
    {
        // if it's an item, we must delegate to the Item class
        if (entityType >= TYPE_ITEM_MIN && entityType <= TYPE_ITEM_MAX) {
            return Item.getClassForType((byte)entityType) != null;
        }

        // otherwise make sure we have a constant defined for this type
        switch (entityType) {
        case TYPE_ROOM:
        case TYPE_PROFILE_WALL:
            return true;
        default:
            return false;
        }
    }

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

    /** The text of this comment. */
    public String text;
}
