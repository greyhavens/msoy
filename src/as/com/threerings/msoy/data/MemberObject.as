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
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.game.data.GameSummary;

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

//    // AUTO-GENERATED: METHODS START
//    /**
//     * Requests that the <code>sceneId</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setSceneId (value :int) :void
//    {
//        var ovalue :int = this.sceneId;
//        requestAttributeChange(
//            SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
//        this.sceneId = value;
//    }
//
//    /**
//     * Requests that the <code>clusterOid</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setClusterOid (value :int) :void
//    {
//        var ovalue :int = this.clusterOid;
//        requestAttributeChange(
//            CLUSTER_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
//        this.clusterOid = value;
//    }
//
//    /**
//     * Requests that the <code>tokens</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setTokens (value :MsoyTokenRing) :void
//    {
//        var ovalue :MsoyTokenRing = this.tokens;
//        requestAttributeChange(
//            TOKENS, value, ovalue);
//        this.tokens = value;
//    }
//
//    /**
//     * Requests that the <code>homeSceneId</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setHomeSceneId (value :int) :void
//    {
//        var ovalue :int = this.homeSceneId;
//        requestAttributeChange(
//            HOME_SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
//        this.homeSceneId = value;
//    }
//
//    /**
//     * Requests that the <code>avatar</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setAvatar (value :MediaDesc) :void
//    {
//        var ovalue :MediaDesc = this.avatar;
//        requestAttributeChange(
//            AVATAR, value, ovalue);
//        this.avatar = value;
//    }
//    // AUTO-GENERATED: METHODS END
//
//    override public function writeObject (out :ObjectOutputStream) :void
//    {
//        throw new Error();
//    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberName = (ins.readObject() as MemberName);
        avrGameId = ins.readInt();
        flow = ins.readInt();
        accFlow = ins.readInt();
        level = ins.readInt();
        humanity = ins.readInt();
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
