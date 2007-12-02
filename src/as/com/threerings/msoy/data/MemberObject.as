//
// $Id$

package com.threerings.msoy.data {

import flash.errors.IllegalOperationError;

import com.threerings.util.Integer;
import com.threerings.util.Name;
import com.threerings.util.Short;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends MsoyBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";

    /** The field name of the <code>avrGameId</code> field. */
    public static const AVR_GAME_ID :String = "avrGameId";

    /** The field name of the <code>humanity</code> field. */
    public static const HUMANITY :String = "humanity";

    /** The field name of the <code>availability</code> field. */
    public static const AVAILABILITY :String = "availability";

    /** The field name of the <code>following</code> field. */
    public static const FOLLOWING :String = "following";

    /** The field name of the <code>followers</code> field. */
    public static const FOLLOWERS :String = "followers";

    /** The field name of the <code>flow</code> field. */
    public static const FLOW :String = "flow";

    /** The field name of the <code>accFlow</code> field. */
    public static const ACC_FLOW :String = "accFlow";

    /** The field name of the <code>level</code> field. */
    public static const LEVEL :String = "level";

    /** The field name of the <code>recentScenes</code> field. */
    public static const RECENT_SCENES :String = "recentScenes";

    /** The field name of the <code>ownedScenes</code> field. */
    public static const OWNED_SCENES :String = "ownedScenes";

    /** The field name of the <code>tokens</code> field. */
    public static const TOKENS :String = "tokens";

    /** The field name of the <code>homeSceneId</code> field. */
    public static const HOME_SCENE_ID :String = "homeSceneId";

    /** The field name of the <code>avatar</code> field. */
    public static const AVATAR :String = "avatar";

    /** The field name of the <code>avatarCache</code> field. */
    public static const AVATAR_CACHE :String = "avatarCache";

    /** The field name of the <code>friends</code> field. */
    public static const FRIENDS :String = "friends";

    /** The field name of the <code>groups</code> field. */
    public static const GROUPS :String = "groups";

    /** The field name of the <code>newMailCount</code> field. */
    public static const NEW_MAIL_COUNT :String = "newMailCount";

    /** The field name of the <code>game</code> field. */
    public static const GAME :String = "game";

    /** The field name of the <code>notifications</code> field. */
    public static const NOTIFICATIONS :String = "notifications";

    /** The field name of the <code>viewOnly</code> field. */
    public static const VIEW_ONLY :String = "viewOnly";
    // AUTO-GENERATED: FIELDS END

    /** An <code>availability</code> status. */
    public static const AVAILABLE :int = 0;

    /** An <code>availability</code> status. */
    public static const FRIENDS_ONLY :int = 1;

    /** An <code>availability</code> status. */
    public static const UNAVAILABLE :int = 2;

    /** The member name and id for this user. */
    public var memberName :MemberName;

    /** The Game ID of the in-avr game that the user is in, if any. */
    public var avrGameId :int;

    /** How much lovely flow we've got jangling around on our person. */
    public var flow :int;

    /** How much total lovely flow we've jangled around on our person. */
    public var accFlow :int;

    /** This user's current level. */
    public var level :int;

    /** Our current assessment of how likely to be human this member is, in [0, 255]. */
    public var humanity :int;

    /** This member's availability for receiving invitations, requests, etc. from other members. */
    public var availability :int = AVAILABLE;

    /** The name of the member this member is following or null. */
    public var following :MemberName;

    /** The names of members following this member. */
    public var followers :DSet;

    /** The recent scenes we've been through. */
    public var recentScenes :DSet;

    /** The scenes we own. */
    public var ownedScenes :DSet;

    /** The tokens defining the access controls for this user. */
    public var tokens :MsoyTokenRing;

    /** The id of the user's home scene. */
    public var homeSceneId :int;

    /** The avatar that the user has chosen, or null for guests. */
    public var avatar :Avatar;

    /** A cache of the user's 5 most recently touched avatars. */
    public var avatarCache :DSet;

    /** The buddies of this player. */
    public var friends :DSet;

    /** The groups of this player. */
    public var groups :DSet;

    /** A field that contains the number of unread messages in our mail inbox. */
    public var newMailCount :int;

    /* The game summary for the game that the player is lobbying for or currently playing. */
    public var game :GameSummary;

    /** The item lists owned by this user. */
    public var lists :DSet;

    /** The set of notifications pending on the member. */
    public var notifications :DSet;

    /** A flag that's true if this member object is only viewing the current scene and should not
     * be rendered in it. */
    public var viewOnly :Boolean;

    /**
     * Return this member's unique id.
     */
    public function getMemberId () :int
    {
        return (memberName == null) ? MemberName.GUEST_ID
                                    : memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return (getMemberId() == MemberName.GUEST_ID);
    }

    /**
     * Returns our home scene id if we're a member, 1 if we're a guest.
     */
    public function getHomeSceneId () :int
    {
        return (homeSceneId == 0) ? 1 : homeSceneId;
    }

    /**
     * Get a sorted list of friends.
     */
    public function getSortedEstablishedFriends () :Array
    {
        var friends :Array = this.friends.toArray();
        friends = friends.sort(
            function (fe1 :FriendEntry, fe2 :FriendEntry) :int {
                return MemberName.BY_DISPLAY_NAME(fe1.name, fe2.name);
            });
        return friends;
    }

    /**
     * Return our assessment of how likely this member is to be human, in [0, 1].
     */
    public function getHumanity () :Number
    {
        return humanity / 255;
    }

    // documentation inherited
    override public function getTokens () :TokenRing
    {
        return tokens;
    }

    override public function getVisibleName () :Name
    {
        return memberName;
    }

    /**
     * Is this user a member of the specified group?
     */
    public function isGroupMember (groupId :int) :Boolean
    {
        return isGroupRank(groupId, GroupMembership.RANK_MEMBER);
    }

    /**
     * Is this user a manager in the specified group?
     */
    public function isGroupManager (groupId :int) :Boolean
    {
        return isGroupRank(groupId, GroupMembership.RANK_MANAGER);
    }

    /**
     * @return true if the user has at least the specified rank in the
     * specified group.
     */
    public function isGroupRank (groupId :int, requiredRank :int) :Boolean
    {
        return getGroupRank(groupId) >= requiredRank;
    }

    /**
     * Get the user's rank in the specified group.
     */
    public function getGroupRank (groupId :int) :int
    {
        if (groups != null) {
            var membInfo :GroupMembership =
                (groups.get(GroupName.makeKey(groupId)) as GroupMembership);
            if (membInfo != null) {
                return membInfo.rank;
            }
        }
        return GroupMembership.RANK_NON_MEMBER;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberName = (ins.readObject() as MemberName);
        avrGameId = ins.readInt();
        flow = ins.readInt();
        accFlow = ins.readInt();
        level = ins.readInt();
        humanity = ins.readInt();
        availability = ins.readInt();
        following = (ins.readObject() as MemberName);
        followers = (ins.readObject() as DSet);
        recentScenes = (ins.readObject() as DSet);
        ownedScenes = (ins.readObject() as DSet);
        tokens = (ins.readObject() as MsoyTokenRing);
        homeSceneId = ins.readInt();
        avatar = (ins.readObject() as Avatar);
        avatarCache = (ins.readObject() as DSet);
        friends = (ins.readObject() as DSet);
        groups = (ins.readObject() as DSet);
        newMailCount = ins.readInt();
        game = (ins.readObject() as GameSummary);
        lists = (ins.readObject() as DSet);
        notifications = (ins.readObject() as DSet);
        viewOnly = ins.readBoolean();
    }
}
}
