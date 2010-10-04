//
// $Id$

package com.threerings.msoy.comment.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Contains runtime data for a comment made by a member on something.
 */
public class Comment
    implements IsSerializable
{
	public static class CommentType
        implements IsSerializable, Streamable
	{
		public static CommentType forItemType (MsoyItemType type)
		{
			return new CommentType(type);
		}

		/** An entity type code indicating a comment on a room. */
		public static final CommentType ROOM = new CommentType(64);

		/** An entity type code indicating a comment on a member's profile. */
		public static final CommentType PROFILE_WALL = new CommentType(65);

		/** An entity type code indicating a comment on a game. */
		public static final CommentType GAME = new CommentType(66);

		public CommentType ()
		{
		}

        public boolean forRoom()
        {
            return ROOM.equals(this);
        }

        public boolean forProfileWall ()
        {
            return PROFILE_WALL.equals(this);
        }

        public boolean forGame ()
        {
            return GAME.equals(this);
        }

        public boolean isValid ()
		{
            return (_type >= 64 && _type <= 66) || isItemType();
		}

		public boolean isItemType ()
		{
			return (_type >= TYPE_ITEM_MIN && _type < TYPE_ITEM_MAX);
		}

		public byte toByte ()
		{
			return _type;
		}

		public MsoyItemType toItemType ()
		{
			return ByteEnumUtil.fromByte(MsoyItemType.class, _type);
		}

        @Override
        public boolean equals (Object o)
        {
            return (this == o) ||
                (o != null && getClass().equals(o.getClass()) && _type == ((CommentType) o)._type);
        }

        @Override
        public int hashCode ()
        {
            return (int) _type;
        }

        @Override
        public String toString ()
        {
            return "[type=" + _type + "]";
        }

        protected CommentType (MsoyItemType itemType)
        {
            _type = itemType.toByte();
        }

        protected CommentType (int type)
        {
            _type = (byte) type;
        }

		protected byte _type;

		/** The minimum entity type code reserved for items. */
		protected static final int TYPE_ITEM_MIN = 1;

		/** The maximum entity type code reserved for items. */
		protected static final int TYPE_ITEM_MAX = 63;
	}

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

    /** The absolute rating of this comment. */
    public int currentRating;

    /** The total number of times this comment has been rated. */
    public int totalRatings;

    /** The text of this comment. */
    public String text;
}
