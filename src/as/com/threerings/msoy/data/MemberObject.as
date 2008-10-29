//
// $Id$

package com.threerings.msoy.data {

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.TokenRing;

import com.threerings.io.ObjectInputStream;

import com.threerings.util.Name;

import com.threerings.msoy.data.MemberExperience;

import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.item.data.all.Avatar;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends MsoyBodyObject
{
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";

    /** The field name of the <code>avrGameId</code> field. */
    public static const AVR_GAME_ID :String = "avrGameId";

    /** The field name of the <code>availability</code> field. */
    public static const AVAILABILITY :String = "availability";

    /** The field name of the <code>following</code> field. */
    public static const FOLLOWING :String = "following";

    /** The field name of the <code>followers</code> field. */
    public static const FOLLOWERS :String = "followers";

    /** The field name of the <code>coins</code> field. */
    public static const COINS :String = "coins";

    /** The field name of the <code>accCoins</code> field. */
    public static const ACC_COINS :String = "accCoins";

    /** The field name of the <code>bars</code> field. */
    public static const BARS :String = "bars";
    
    /** The field name of the <code>level</code> field. */
    public static const LEVEL :String = "level";

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

    /** The field name of the <code>gateways</code> field. */
    public static const GATEWAYS :String = "gateways";

    /** The field name of the <code>aimContacts</code> field. */
    public static const IM_CONTACTS :String = "imContacts";

    /** The field name of the <code>groups</code> field. */
    public static const GROUPS :String = "groups";

    /** The field name of the <code>newMailCount</code> field. */
    public static const NEW_MAIL_COUNT :String = "newMailCount";

    /** The field name of the <code>game</code> field. */
    public static const GAME :String = "game";

    /** The field name of the <code>walkingId</code> field. */
    public static const WALKING_ID :String = "walkingId";

    /** The field name of the <code>headline</code> field. */
    public static const HEADLINE :String = "headline";

    /** The field name of the <code>visitorInfo</code> field. */
    public static const VISITOR_INFO :String = "visitorInfo";

    /** The field name of the <code>onTour</code> field. */
    public static const ON_TOUR :String = "onTour";

    /** A message sent by the server to denote a notification to be displayed.
     * Format: [ Notification ]. */
    public static const NOTIFICATION :String = "notification";

    /** An <code>availability</code> status. */
    public static const AVAILABLE :int = 0;

    /** An <code>availability</code> status. */
    public static const FRIENDS_ONLY :int = 1;

    /** An <code>availability</code> status. */
    public static const UNAVAILABLE :int = 2;

    /** The member name and id for this user. */
    public var memberName :VizMemberName;

    /** The id of the currently active AVR game for this user, or 0 for none. Fun fact: this field
     * actually records the *most recent* avrg for the user and therefore is not always equal
     * to <code>(game != null && game.avrGame) ? game.gameId : 0</code>. The reason for this is
     * that the client may wish to rejoin a game after an involuntary disconnect.
     * TODO: Decide if this functionality is still needed. */
    public var avrGameId :int;

    /** How many coins we've got jangling around on our person. */
    public var coins :int;

    /** How many coins total we've jangled around on our person. */
    public var accCoins :int;

    /** How many bars total this member has. */
    public var bars :int;
    
    /** This user's current level. */
    public var level :int;

    /** This member's availability for receiving invitations, requests, etc. from other members. */
    public var availability :int = AVAILABLE;

    /** The name of the member this member is following or null. */
    public var following :MemberName;

    /** The names of members following this member. */
    public var followers :DSet;

//    /** The recent scenes we've been through. */
//    public var recentScenes :DSet;
//
//    /** The scenes we own. */
//    public var ownedScenes :DSet;

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

    /** The IM gateways available to this player. */
    public var gateways :DSet;

    /** The IM contacts of this player. */
    public var imContacts :DSet;

    /** The groups of this player. */
    public var groups :DSet;

    /** A field that contains the number of unread messages in our mail inbox. */
    public var newMailCount :int;

    /* The game summary for the game that the player is lobbying for or currently playing. */
    public var game :GameSummary;

    /** The item lists owned by this user. */
    public var lists :DSet;

    /** If this member is currently walking a pet, the id of the pet being walked, else 0. */
    public var walkingId :int;

    /** The headline/status of this player. */
    public var headline :String;

    /** Player's tracking information. */
    public var visitorInfo :VisitorInfo;

    /** Whether this player is on the "whirled tour". */
    public var onTour :Boolean;

    /** Experiences this player has had. */
    public var experiences :DSet; /* of */ MemberExperience;
    
    /**
     * Return this member's unique id.
     */
    public function getMemberId () :int
    {
        return memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return memberName.isGuest();
    }

    /**
     * Return true if this user is only viewing the scene and should not be rendered within it.
     */
    public function isViewer () :Boolean
    {
        return memberName.isViewer();
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
     * Tests if this member has any friends online.
     */
    public function hasOnlineFriends () :Boolean
    {
        for each (var fe :FriendEntry in friends.toArray()) {
            if (fe.online) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a sorted list of gateways.
     */
    public function getSortedGateways () :Array
    {
        var gateways :Array = this.gateways.toArray();
        gateways = gateways.sort(
            function (g1 :GatewayEntry, g2 :GatewayEntry) :int {
                return g1.compareTo(g2);
            });
        return gateways;
    }

    /**
     * Get a sorted list of aim contacts for a specified gateway.
     */
    public function getSortedImContacts (gateway :String) :Array
    {
        var contacts :Array = this.imContacts.toArray();
        contacts = contacts.filter(
            function (ce :ContactEntry, index :int, array :Array) :Boolean {
                return ce.getGateway() == gateway;
            });
        contacts = contacts.sort(
            function (ce1 :ContactEntry, ce2 :ContactEntry) :int {
                return ce1.compareTo(ce2);
            });
        return contacts;
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
            var membInfo :GroupMembership = (groups.get(groupId) as GroupMembership);
            if (membInfo != null) {
                return membInfo.rank;
            }
        }
        return GroupMembership.RANK_NON_MEMBER;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberName = VizMemberName(ins.readObject());
        avrGameId = ins.readInt();
        coins = ins.readInt();
        accCoins = ins.readInt();
        bars = ins.readInt();
        level = ins.readInt();
        availability = ins.readInt();
        following = MemberName(ins.readObject());
        followers = DSet(ins.readObject());
//        recentScenes = DSet(ins.readObject());
//        ownedScenes = DSet(ins.readObject());
        tokens = MsoyTokenRing(ins.readObject());
        homeSceneId = ins.readInt();
        avatar = Avatar(ins.readObject());
        avatarCache = DSet(ins.readObject());
        friends = DSet(ins.readObject());
        gateways = DSet(ins.readObject());
        imContacts = DSet(ins.readObject());
        groups = DSet(ins.readObject());
        newMailCount = ins.readInt();
        game = GameSummary(ins.readObject());
        lists = DSet(ins.readObject());
        walkingId = ins.readInt();
        headline = ins.readField(String) as String;
        visitorInfo = VisitorInfo(ins.readObject());
        onTour = ins.readBoolean();
        experiences = DSet(ins.readObject());
    }
}
}
