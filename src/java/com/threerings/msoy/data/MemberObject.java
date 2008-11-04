//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.ItemListInfo;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.ObserverInfo;

import static com.threerings.msoy.Log.log;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends MsoyBodyObject
    implements MsoyUserObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>avrGameId</code> field. */
    public static final String AVR_GAME_ID = "avrGameId";

    /** The field name of the <code>coins</code> field. */
    public static final String COINS = "coins";

    /** The field name of the <code>accCoins</code> field. */
    public static final String ACC_COINS = "accCoins";

    /** The field name of the <code>bars</code> field. */
    public static final String BARS = "bars";

    /** The field name of the <code>level</code> field. */
    public static final String LEVEL = "level";

    /** The field name of the <code>availability</code> field. */
    public static final String AVAILABILITY = "availability";

    /** The field name of the <code>following</code> field. */
    public static final String FOLLOWING = "following";

    /** The field name of the <code>followers</code> field. */
    public static final String FOLLOWERS = "followers";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>homeSceneId</code> field. */
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>avatarCache</code> field. */
    public static final String AVATAR_CACHE = "avatarCache";

    /** The field name of the <code>friends</code> field. */
    public static final String FRIENDS = "friends";

    /** The field name of the <code>gateways</code> field. */
    public static final String GATEWAYS = "gateways";

    /** The field name of the <code>imContacts</code> field. */
    public static final String IM_CONTACTS = "imContacts";

    /** The field name of the <code>groups</code> field. */
    public static final String GROUPS = "groups";

    /** The field name of the <code>newMailCount</code> field. */
    public static final String NEW_MAIL_COUNT = "newMailCount";

    /** The field name of the <code>game</code> field. */
    public static final String GAME = "game";

    /** The field name of the <code>lists</code> field. */
    public static final String LISTS = "lists";

    /** The field name of the <code>walkingId</code> field. */
    public static final String WALKING_ID = "walkingId";

    /** The field name of the <code>headline</code> field. */
    public static final String HEADLINE = "headline";

    /** The field name of the <code>visitorInfo</code> field. */
    public static final String VISITOR_INFO = "visitorInfo";

    /** The field name of the <code>onTour</code> field. */
    public static final String ON_TOUR = "onTour";

    /** The field name of the <code>experiences</code> field. */
    public static final String EXPERIENCES = "experiences";

    /** The field name of the <code>greeter</code> field. */
    public static final String GREETER = "greeter";
    // AUTO-GENERATED: FIELDS END

    /** A message sent by the server to denote a notification to be displayed.
     * Format: [ Notification ]. */
    public static final String NOTIFICATION = "notification";

    /** The ideal size of the avatar cache. */
    public static final int AVATAR_CACHE_SIZE = 5;

    /** An {@link #availability} status. */
    public static final int AVAILABLE = 0;

    /** An {@link #availability} status. */
    public static final int FRIENDS_ONLY = 1;

    /** An {@link #availability} status. */
    public static final int UNAVAILABLE = 2;

    /** The name and id information for this user. */
    public VizMemberName memberName;

    /** The id of the currently active AVR game for this user, or 0 for none. Fun fact: this field
     * actually records the *most recent* avrg for the user and therefore is not always equal
     * to <code>(game != null && game.avrGame) ? game.gameId : 0</code>. The reason for this is
     * that the client may wish to rejoin a game after an involuntary disconnect.
     * TODO: Decide if this functionality is still needed. */
    public int avrGameId;

    /** How many coins we've got jangling around on our person. */
    public int coins;

    /** How many coins total we've jangled around on our person. */
    public int accCoins;

    /** The number of bars the member has currently in their account. */
    public int bars;

    /** This user's current level. */
    public int level;

    /** This member's availability for receiving invitations, requests, etc. from other members. */
    public int availability = AVAILABLE;

    /** The name of the member this member is following or null. */
    public MemberName following;

    /** The names of members following this member. */
    public DSet<MemberName> followers = new DSet<MemberName>();

//    /** The recent scenes we've been through. */
//    public DSet<SceneBookmarkEntry> recentScenes = new DSet<SceneBookmarkEntry>();
//
//    /** The scenes we own. */
//    public DSet<SceneBookmarkEntry> ownedScenes = new DSet<SceneBookmarkEntry>();

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The id of the user's home scene. */
    public int homeSceneId;

    /** The avatar that the user has chosen, or null for guests. */
    public Avatar avatar;

    /** A cache of the user's 5 most recently touched avatars. */
    public DSet<Avatar> avatarCache;

    /** The friends of this player. */
    public DSet<FriendEntry> friends = new DSet<FriendEntry>();

    /** The IM gateways available to this player. */
    public DSet<GatewayEntry> gateways = new DSet<GatewayEntry>();

    /** The IM contacts of this player. */
    public DSet<ContactEntry> imContacts = new DSet<ContactEntry>();

    /** The groups of this player. */
    public DSet<GroupMembership> groups;

    /** A field that contains the number of unread messages in our mail inbox. */
    public int newMailCount;

    /** The game summary for the game that the player is lobbying for or currently playing. */
    public GameSummary game;

    /** The item lists owned by this user. */
    public DSet<ItemListInfo> lists = new DSet<ItemListInfo>();

    /** If this member is currently walking a pet, the id of the pet being walked, else 0. */
    public int walkingId;

    /** The headline/status of this member. */
    public String headline;

    /** Player's tracking information. */
    public VisitorInfo visitorInfo;

    /** Whether this player is on the "whirled tour". We could also check whether touredRooms
     * is null, but that's not sent to the client. */
    public boolean onTour;
    
    /** List of experiences this member has had recently. */
    public DSet<MemberExperience> experiences = new DSet<MemberExperience>();

    /** Set if the user has opted to help newcomers to whirled. */
    public boolean greeter;

    /**
     * Return true if this user is a guest.
     */
    public boolean isGuest ()
    {
        return memberName.isGuest();
    }

    /**
     * Return true if this user is only viewing the scene and should not be rendered within it.
     */
    public boolean isViewer ()
    {
        return memberName.isViewer();
    }

    /**
     * Returns  our home scene id or 1 (Brave New Whirled) if we have none.
     */
    public int getHomeSceneId ()
    {
        return (homeSceneId == 0) ? 1 : homeSceneId;
    }

    /**
     * Get the media to use as our headshot.
     */
    public MediaDesc getHeadShotMedia ()
    {
        return memberName.getPhoto();
    }

    /**
     * Returns true if the specified member is our friend.
     */
    public boolean isFriend (int memberId)
    {
        return friends.containsKey(memberId);
    }

    /**
     * Is this user a member of the specified group?
     */
    public boolean isGroupMember (int groupId)
    {
        return isGroupRank(groupId, GroupMembership.RANK_MEMBER);
    }

    /**
     * Is this user a manager in the specified group?
     */
    public boolean isGroupManager (int groupId)
    {
        return isGroupRank(groupId, GroupMembership.RANK_MANAGER);
    }

    /**
     * @return true if the user has at least the specified rank in the specified group.
     */
    public boolean isGroupRank (int groupId, byte requiredRank)
    {
        return getGroupRank(groupId) >= requiredRank;
    }

    /**
     * Get the user's rank in the specified group.
     */
    public byte getGroupRank (int groupId)
    {
        if (groups != null) {
            GroupMembership membInfo = groups.get(groupId);
            if (membInfo != null) {
                return membInfo.rank;
            }
        }
        return GroupMembership.RANK_NON_MEMBER;
    }

    /**
     * Returns {@link #visitorInfo}.id but logs a warning and stack trace if visitorInfo is null.
     */
    public String getVisitorId ()
    {
        if (isViewer()) {
            return "";
        } else if (visitorInfo == null) {
            log.warning("Member missing visitorInfo", "who", who(), new Exception());
            return "";
        } else {
            return visitorInfo.id;
        }
    }

//    /**
//     * Add the specified scene to the recent scene list for this user.
//     */
//    public void addToRecentScenes (int sceneId, String name)
//    {
//        SceneBookmarkEntry newEntry = new SceneBookmarkEntry(
//            sceneId, name, System.currentTimeMillis());
//
//        SceneBookmarkEntry oldest = null;
//        for (SceneBookmarkEntry sbe : recentScenes) {
//            if (sbe.sceneId == sceneId) {
//                updateRecentScenes(newEntry);
//                return;
//            }
//            if (oldest == null || oldest.lastVisit > sbe.lastVisit) {
//                oldest = sbe;
//            }
//        }
//
//        int size = recentScenes.size();
//        if (size < MAX_RECENT_SCENES) {
//            addToRecentScenes(newEntry);
//
//        } else {
//            startTransaction();
//            try {
//                removeFromRecentScenes(oldest.getKey());
//                addToRecentScenes(newEntry);
//            } finally {
//                commitTransaction();
//            }
//        }
//    }

    /**
     * Returns true if this member is accepting communications from the specified member, false
     * otherwise.
     */
    public boolean isAvailableTo (int communicatorId)
    {
        switch (availability) {
        default:
        case AVAILABLE:
            return true;
        case UNAVAILABLE:
            return false;
        case FRIENDS_ONLY:
            return friends.containsKey(communicatorId);
        }
    }

    /**
     * Clears out information that is not relevant on the receiving peer server for a member object
     * that has just been forwarded. (It would be a smidgen more efficient to do this on the
     * sending server but we'd have to clone the MemberObject first and we don't want to force
     * MemberObject to be cloneable.)
     */
    public void clearForwardedObject ()
    {
        _oid = 0;
        location = null;
    }

    /**
     * Publishes this member's updated display name to their member object.
     */
    public void updateDisplayName (String displayName)
    {
        updateDisplayName(displayName, null);
    }

    /**
     * Publishes this member's updated display name and profile image to their member object.
     */
    public void updateDisplayName (String displayName, MediaDesc image)
    {
        if (image == null) {
            image = memberName.getPhoto();
        }
        setMemberName(new VizMemberName(displayName, getMemberId(), image));
    }

    // from interface MsoyUserObject
    public MemberName getMemberName ()
    {
        return memberName;
    }

    // from interface MsoyUserObject
    public int getMemberId ()
    {
        return memberName.getMemberId();
    }

    @Override // from MsoyBodyObject
    public boolean canEnterScene (int sceneId, int ownerId, byte ownerType, byte accessControl)
    {
        boolean hasRights = false;

        if (ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
            switch (accessControl) {
            case MsoySceneModel.ACCESS_EVERYONE: hasRights = true; break;
            case MsoySceneModel.ACCESS_OWNER_ONLY: hasRights = isGroupManager(ownerId); break;
            case MsoySceneModel.ACCESS_OWNER_AND_FRIENDS: hasRights = isGroupMember(ownerId); break;
            }

        } else {
            switch (accessControl) {
            case MsoySceneModel.ACCESS_EVERYONE: hasRights = true; break;
            case MsoySceneModel.ACCESS_OWNER_ONLY: hasRights = (getMemberId() == ownerId); break;
            case MsoySceneModel.ACCESS_OWNER_AND_FRIENDS:
                hasRights = (getMemberId() == ownerId) || isFriend(ownerId);
                break;
            }
        }

        if (!hasRights && tokens.isSupport()) {
            log.info("Granting support+ access to inaccessible scene [who=" + who() +
                     ", sceneId=" + sceneId + ", type=" + ownerType + "].");
            hasRights = true;
        }

        return hasRights;
    }

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return isViewer() ? new ObserverInfo(this) : new MemberInfo(this);
    }

    @Override // from BodyObject
    public TokenRing getTokens ()
    {
        return tokens;
    }

    @Override // from BodyObject
    public Name getVisibleName ()
    {
        return memberName;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>memberName</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMemberName (VizMemberName value)
    {
        VizMemberName ovalue = this.memberName;
        requestAttributeChange(
            MEMBER_NAME, value, ovalue);
        this.memberName = value;
    }

    /**
     * Requests that the <code>avrGameId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAvrGameId (int value)
    {
        int ovalue = this.avrGameId;
        requestAttributeChange(
            AVR_GAME_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.avrGameId = value;
    }

    /**
     * Requests that the <code>coins</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setCoins (int value)
    {
        int ovalue = this.coins;
        requestAttributeChange(
            COINS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.coins = value;
    }

    /**
     * Requests that the <code>accCoins</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAccCoins (int value)
    {
        int ovalue = this.accCoins;
        requestAttributeChange(
            ACC_COINS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.accCoins = value;
    }

    /**
     * Requests that the <code>bars</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBars (int value)
    {
        int ovalue = this.bars;
        requestAttributeChange(
            BARS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.bars = value;
    }

    /**
     * Requests that the <code>level</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLevel (int value)
    {
        int ovalue = this.level;
        requestAttributeChange(
            LEVEL, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.level = value;
    }

    /**
     * Requests that the <code>availability</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAvailability (int value)
    {
        int ovalue = this.availability;
        requestAttributeChange(
            AVAILABILITY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.availability = value;
    }

    /**
     * Requests that the <code>following</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFollowing (MemberName value)
    {
        MemberName ovalue = this.following;
        requestAttributeChange(
            FOLLOWING, value, ovalue);
        this.following = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>followers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToFollowers (MemberName elem)
    {
        requestEntryAdd(FOLLOWERS, followers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>followers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromFollowers (Comparable<?> key)
    {
        requestEntryRemove(FOLLOWERS, followers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>followers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateFollowers (MemberName elem)
    {
        requestEntryUpdate(FOLLOWERS, followers, elem);
    }

    /**
     * Requests that the <code>followers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setFollowers (DSet<MemberName> value)
    {
        requestAttributeChange(FOLLOWERS, value, this.followers);
        DSet<MemberName> clone = (value == null) ? null : value.typedClone();
        this.followers = clone;
    }

    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTokens (MsoyTokenRing value)
    {
        MsoyTokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the <code>homeSceneId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHomeSceneId (int value)
    {
        int ovalue = this.homeSceneId;
        requestAttributeChange(
            HOME_SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.homeSceneId = value;
    }

    /**
     * Requests that the <code>avatar</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAvatar (Avatar value)
    {
        Avatar ovalue = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>avatarCache</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToAvatarCache (Avatar elem)
    {
        requestEntryAdd(AVATAR_CACHE, avatarCache, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>avatarCache</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromAvatarCache (Comparable<?> key)
    {
        requestEntryRemove(AVATAR_CACHE, avatarCache, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>avatarCache</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateAvatarCache (Avatar elem)
    {
        requestEntryUpdate(AVATAR_CACHE, avatarCache, elem);
    }

    /**
     * Requests that the <code>avatarCache</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setAvatarCache (DSet<Avatar> value)
    {
        requestAttributeChange(AVATAR_CACHE, value, this.avatarCache);
        DSet<Avatar> clone = (value == null) ? null : value.typedClone();
        this.avatarCache = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>friends</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToFriends (FriendEntry elem)
    {
        requestEntryAdd(FRIENDS, friends, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>friends</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromFriends (Comparable<?> key)
    {
        requestEntryRemove(FRIENDS, friends, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>friends</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateFriends (FriendEntry elem)
    {
        requestEntryUpdate(FRIENDS, friends, elem);
    }

    /**
     * Requests that the <code>friends</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setFriends (DSet<FriendEntry> value)
    {
        requestAttributeChange(FRIENDS, value, this.friends);
        DSet<FriendEntry> clone = (value == null) ? null : value.typedClone();
        this.friends = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>gateways</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGateways (GatewayEntry elem)
    {
        requestEntryAdd(GATEWAYS, gateways, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>gateways</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGateways (Comparable<?> key)
    {
        requestEntryRemove(GATEWAYS, gateways, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>gateways</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGateways (GatewayEntry elem)
    {
        requestEntryUpdate(GATEWAYS, gateways, elem);
    }

    /**
     * Requests that the <code>gateways</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGateways (DSet<GatewayEntry> value)
    {
        requestAttributeChange(GATEWAYS, value, this.gateways);
        DSet<GatewayEntry> clone = (value == null) ? null : value.typedClone();
        this.gateways = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>imContacts</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToImContacts (ContactEntry elem)
    {
        requestEntryAdd(IM_CONTACTS, imContacts, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>imContacts</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromImContacts (Comparable<?> key)
    {
        requestEntryRemove(IM_CONTACTS, imContacts, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>imContacts</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateImContacts (ContactEntry elem)
    {
        requestEntryUpdate(IM_CONTACTS, imContacts, elem);
    }

    /**
     * Requests that the <code>imContacts</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setImContacts (DSet<ContactEntry> value)
    {
        requestAttributeChange(IM_CONTACTS, value, this.imContacts);
        DSet<ContactEntry> clone = (value == null) ? null : value.typedClone();
        this.imContacts = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>groups</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGroups (GroupMembership elem)
    {
        requestEntryAdd(GROUPS, groups, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>groups</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGroups (Comparable<?> key)
    {
        requestEntryRemove(GROUPS, groups, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>groups</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGroups (GroupMembership elem)
    {
        requestEntryUpdate(GROUPS, groups, elem);
    }

    /**
     * Requests that the <code>groups</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGroups (DSet<GroupMembership> value)
    {
        requestAttributeChange(GROUPS, value, this.groups);
        DSet<GroupMembership> clone = (value == null) ? null : value.typedClone();
        this.groups = clone;
    }

    /**
     * Requests that the <code>newMailCount</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNewMailCount (int value)
    {
        int ovalue = this.newMailCount;
        requestAttributeChange(
            NEW_MAIL_COUNT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newMailCount = value;
    }

    /**
     * Requests that the <code>game</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGame (GameSummary value)
    {
        GameSummary ovalue = this.game;
        requestAttributeChange(
            GAME, value, ovalue);
        this.game = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>lists</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToLists (ItemListInfo elem)
    {
        requestEntryAdd(LISTS, lists, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>lists</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromLists (Comparable<?> key)
    {
        requestEntryRemove(LISTS, lists, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>lists</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateLists (ItemListInfo elem)
    {
        requestEntryUpdate(LISTS, lists, elem);
    }

    /**
     * Requests that the <code>lists</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setLists (DSet<ItemListInfo> value)
    {
        requestAttributeChange(LISTS, value, this.lists);
        DSet<ItemListInfo> clone = (value == null) ? null : value.typedClone();
        this.lists = clone;
    }

    /**
     * Requests that the <code>walkingId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setWalkingId (int value)
    {
        int ovalue = this.walkingId;
        requestAttributeChange(
            WALKING_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.walkingId = value;
    }

    /**
     * Requests that the <code>headline</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHeadline (String value)
    {
        String ovalue = this.headline;
        requestAttributeChange(
            HEADLINE, value, ovalue);
        this.headline = value;
    }

    /**
     * Requests that the <code>visitorInfo</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setVisitorInfo (VisitorInfo value)
    {
        VisitorInfo ovalue = this.visitorInfo;
        requestAttributeChange(
            VISITOR_INFO, value, ovalue);
        this.visitorInfo = value;
    }

    /**
     * Requests that the <code>onTour</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setOnTour (boolean value)
    {
        boolean ovalue = this.onTour;
        requestAttributeChange(
            ON_TOUR, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.onTour = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>experiences</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToExperiences (MemberExperience elem)
    {
        requestEntryAdd(EXPERIENCES, experiences, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>experiences</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromExperiences (Comparable<?> key)
    {
        requestEntryRemove(EXPERIENCES, experiences, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>experiences</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateExperiences (MemberExperience elem)
    {
        requestEntryUpdate(EXPERIENCES, experiences, elem);
    }

    /**
     * Requests that the <code>experiences</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setExperiences (DSet<MemberExperience> value)
    {
        requestAttributeChange(EXPERIENCES, value, this.experiences);
        DSet<MemberExperience> clone = (value == null) ? null : value.typedClone();
        this.experiences = clone;
    }

    /**
     * Requests that the <code>greeter</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGreeter (boolean value)
    {
        boolean ovalue = this.greeter;
        requestAttributeChange(
            GREETER, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.greeter = value;
    }
    // AUTO-GENERATED: METHODS END

    @Override // from BodyObject
    protected void addWhoData (StringBuilder buf)
    {
        buf.append("id=").append(getMemberId()).append(" oid=");
        super.addWhoData(buf);
    }

//    /** Limits the number of recent scenes tracked in {@link #recentScenes}. */
//    protected static final int MAX_RECENT_SCENES = 10;
}
