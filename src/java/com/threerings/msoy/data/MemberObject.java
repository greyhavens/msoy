//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.Streamable;
import com.threerings.parlor.game.data.GameObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.stats.data.StatSet;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldMemberInfo;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;

import com.threerings.msoy.game.data.GameMemberInfo;
import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.notify.data.Notification;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends MsoyBodyObject
    implements MsoyUserObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>worldGameOid</code> field. */
    public static final String WORLD_GAME_OID = "worldGameOid";

    /** The field name of the <code>worldGameCfg</code> field. */
    public static final String WORLD_GAME_CFG = "worldGameCfg";

    /** The field name of the <code>flow</code> field. */
    public static final String FLOW = "flow";

    /** The field name of the <code>accFlow</code> field. */
    public static final String ACC_FLOW = "accFlow";

    /** The field name of the <code>level</code> field. */
    public static final String LEVEL = "level";

    /** The field name of the <code>humanity</code> field. */
    public static final String HUMANITY = "humanity";

    /** The field name of the <code>recentScenes</code> field. */
    public static final String RECENT_SCENES = "recentScenes";

    /** The field name of the <code>ownedScenes</code> field. */
    public static final String OWNED_SCENES = "ownedScenes";

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

    /** The field name of the <code>groups</code> field. */
    public static final String GROUPS = "groups";

    /** The field name of the <code>hasNewMail</code> field. */
    public static final String HAS_NEW_MAIL = "hasNewMail";

    /** The field name of the <code>game</code> field. */
    public static final String GAME = "game";

    /** The field name of the <code>lists</code> field. */
    public static final String LISTS = "lists";

    /** The field name of the <code>notifications</code> field. */
    public static final String NOTIFICATIONS = "notifications";
    // AUTO-GENERATED: FIELDS END

    /** The ideal size of the avatar cache. */
    public static final int AVATAR_CACHE_SIZE = 5;

    /** The name and id information for this user. */
    public MemberName memberName;

    /** The object ID of the in-world game that the user is in, or 0. */
    public int worldGameOid;

    /** The config that goes along with the world game oid, or null. This is not typed
     * WorldGameConfig to avoid introducing a dependency on all the game code in the MemberObject
     * which is used by numerous clients that don't care about game stuff. */
    public Streamable worldGameCfg;

    /** How much lovely flow we've got jangling around on our person. */
    public int flow;

    /** How much total lovely flow we've jangled around on our person. */
    public int accFlow;

    /** This user's current level. */
    public int level;

    /** Our current assessment of how likely to be human this member is, in [0, {@link
     * MsoyCodes#MAX_HUMANITY}]. */
    public int humanity;

    /** The recent scenes we've been through. */
    public DSet<SceneBookmarkEntry> recentScenes = new DSet<SceneBookmarkEntry>();

    /** The scenes we own. */
    public DSet<SceneBookmarkEntry> ownedScenes = new DSet<SceneBookmarkEntry>();

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

    /** The groups of this player. */
    public DSet<GroupMembership> groups;

    /** A flag that's true if this member has unread mail. */
    public boolean hasNewMail;

    /** The game summary for the game that the player is lobbying for or currently playing. */
    public GameSummary game;

    /** The item lists owned by this user. */
    public DSet<ItemListInfo> lists = new DSet<ItemListInfo>();

    /** The set of notifications pending on the member object. */
    public DSet<Notification> notifications = new DSet<Notification>();

    /** Statistics tracked for this player. */
    public transient StatSet stats;

    /**
     * Return true if this user is merely a guest.
     */
    public boolean isGuest ()
    {
        return (getMemberId() == MemberName.GUEST_ID);
    }

    /**
     * Get the media to use as our headshot.
     */
    public MediaDesc getHeadShotMedia ()
    {
        if (avatar != null) {
            return avatar.getThumbnailMedia();
        }
        return Avatar.getDefaultThumbnailMediaFor(Item.AVATAR);
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
            GroupMembership membInfo = groups.get(GroupName.makeKey(groupId));
            if (membInfo != null) {
                return membInfo.rank;
            }
        }
        return GroupMembership.RANK_NON_MEMBER;
    }

    /**
     * Add the specified scene to the recent scene list for this user.
     */
    public void addToRecentScenes (int sceneId, String name)
    {
        SceneBookmarkEntry newEntry = new SceneBookmarkEntry(
            sceneId, name, System.currentTimeMillis());

        SceneBookmarkEntry oldest = null;
        for (SceneBookmarkEntry sbe : recentScenes) {
            if (sbe.sceneId == sceneId) {
                updateRecentScenes(newEntry);
                return;
            }
            if (oldest == null || oldest.lastVisit > sbe.lastVisit) {
                oldest = sbe;
            }
        }

        int size = recentScenes.size();
        if (size < MAX_RECENT_SCENES) {
            addToRecentScenes(newEntry);

        } else {
            startTransaction();
            try {
                removeFromRecentScenes(oldest.getKey());
                addToRecentScenes(newEntry);
            } finally {
                commitTransaction();
            }
        }
    }

    /**
     * Add the specified notification instance to the member's notification queue.  The
     * notification's id field will be modify with a unique value.
     */
    public void notify (Notification notification)
    {
        notification.id = ++_notificationCount;
        addToNotifications(notification);
    }

    /**
     * Processes notification acknowledgement by removing in from the user's queue.  Returns true
     * if the notification was removed successfully, false otherwise (e.g. if the notification was
     * not present in the queue).
     */
    public boolean acknowledgeNotification (int id)
    {
        if (notifications.containsKey(id)) {
            removeFromNotifications(id);
            return true;
        } else {
            return false;
        }
    }

    // from interface MsoyUserObject
    public MemberName getMemberName ()
    {
        return memberName;
    }

    // from interface MsoyUserObject
    public int getMemberId ()
    {
        return (memberName == null) ? MemberName.GUEST_ID : memberName.getMemberId();
    }

    // from interface MsoyUserObject
    public float getHumanity ()
    {
        return humanity / (float)MsoyCodes.MAX_HUMANITY;
    }

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        if (plobj instanceof RoomObject) {
            return new WorldMemberInfo(this);
        } else if (plobj instanceof GameObject) {
            return new GameMemberInfo(this);
        } else {
            return new MemberInfo(this);
        }
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
    public void setMemberName (MemberName value)
    {
        MemberName ovalue = this.memberName;
        requestAttributeChange(
            MEMBER_NAME, value, ovalue);
        this.memberName = value;
    }

    /**
     * Requests that the <code>worldGameOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setWorldGameOid (int value)
    {
        int ovalue = this.worldGameOid;
        requestAttributeChange(
            WORLD_GAME_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.worldGameOid = value;
    }

    /**
     * Requests that the <code>worldGameCfg</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setWorldGameCfg (Streamable value)
    {
        Streamable ovalue = this.worldGameCfg;
        requestAttributeChange(
            WORLD_GAME_CFG, value, ovalue);
        this.worldGameCfg = value;
    }

    /**
     * Requests that the <code>flow</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFlow (int value)
    {
        int ovalue = this.flow;
        requestAttributeChange(
            FLOW, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.flow = value;
    }

    /**
     * Requests that the <code>accFlow</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAccFlow (int value)
    {
        int ovalue = this.accFlow;
        requestAttributeChange(
            ACC_FLOW, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.accFlow = value;
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
     * Requests that the <code>humanity</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHumanity (int value)
    {
        int ovalue = this.humanity;
        requestAttributeChange(
            HUMANITY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.humanity = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>recentScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToRecentScenes (SceneBookmarkEntry elem)
    {
        requestEntryAdd(RECENT_SCENES, recentScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>recentScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromRecentScenes (Comparable key)
    {
        requestEntryRemove(RECENT_SCENES, recentScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>recentScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateRecentScenes (SceneBookmarkEntry elem)
    {
        requestEntryUpdate(RECENT_SCENES, recentScenes, elem);
    }

    /**
     * Requests that the <code>recentScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setRecentScenes (DSet<com.threerings.msoy.data.all.SceneBookmarkEntry> value)
    {
        requestAttributeChange(RECENT_SCENES, value, this.recentScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.all.SceneBookmarkEntry> clone =
            (value == null) ? null : value.typedClone();
        this.recentScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>ownedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToOwnedScenes (SceneBookmarkEntry elem)
    {
        requestEntryAdd(OWNED_SCENES, ownedScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>ownedScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromOwnedScenes (Comparable key)
    {
        requestEntryRemove(OWNED_SCENES, ownedScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>ownedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateOwnedScenes (SceneBookmarkEntry elem)
    {
        requestEntryUpdate(OWNED_SCENES, ownedScenes, elem);
    }

    /**
     * Requests that the <code>ownedScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setOwnedScenes (DSet<com.threerings.msoy.data.all.SceneBookmarkEntry> value)
    {
        requestAttributeChange(OWNED_SCENES, value, this.ownedScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.all.SceneBookmarkEntry> clone =
            (value == null) ? null : value.typedClone();
        this.ownedScenes = clone;
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
    public void removeFromAvatarCache (Comparable key)
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
    public void setAvatarCache (DSet<com.threerings.msoy.item.data.all.Avatar> value)
    {
        requestAttributeChange(AVATAR_CACHE, value, this.avatarCache);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.item.data.all.Avatar> clone =
            (value == null) ? null : value.typedClone();
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
    public void removeFromFriends (Comparable key)
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
    public void setFriends (DSet<com.threerings.msoy.data.all.FriendEntry> value)
    {
        requestAttributeChange(FRIENDS, value, this.friends);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.all.FriendEntry> clone =
            (value == null) ? null : value.typedClone();
        this.friends = clone;
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
    public void removeFromGroups (Comparable key)
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
    public void setGroups (DSet<com.threerings.msoy.data.all.GroupMembership> value)
    {
        requestAttributeChange(GROUPS, value, this.groups);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.all.GroupMembership> clone =
            (value == null) ? null : value.typedClone();
        this.groups = clone;
    }

    /**
     * Requests that the <code>hasNewMail</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHasNewMail (boolean value)
    {
        boolean ovalue = this.hasNewMail;
        requestAttributeChange(
            HAS_NEW_MAIL, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.hasNewMail = value;
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
    public void removeFromLists (Comparable key)
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
    public void setLists (DSet<com.threerings.msoy.item.data.all.ItemListInfo> value)
    {
        requestAttributeChange(LISTS, value, this.lists);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.item.data.all.ItemListInfo> clone =
            (value == null) ? null : value.typedClone();
        this.lists = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>notifications</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToNotifications (Notification elem)
    {
        requestEntryAdd(NOTIFICATIONS, notifications, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>notifications</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromNotifications (Comparable key)
    {
        requestEntryRemove(NOTIFICATIONS, notifications, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>notifications</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateNotifications (Notification elem)
    {
        requestEntryUpdate(NOTIFICATIONS, notifications, elem);
    }

    /**
     * Requests that the <code>notifications</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setNotifications (DSet<com.threerings.msoy.notify.data.Notification> value)
    {
        requestAttributeChange(NOTIFICATIONS, value, this.notifications);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.notify.data.Notification> clone =
            (value == null) ? null : value.typedClone();
        this.notifications = clone;
    }
    // AUTO-GENERATED: METHODS END

    @Override // from BodyObject
    protected void addWhoData (StringBuilder buf)
    {
        buf.append("id=").append(getMemberId()).append(" oid=");
        super.addWhoData(buf);
    }

    /** Used for tracking the number of notifications processed on the member object. */
    protected transient int _notificationCount = 0;

    /** Limits the number of recent scenes tracked in {@link #recentScenes}. */
    protected static final int MAX_RECENT_SCENES = 10;
}
