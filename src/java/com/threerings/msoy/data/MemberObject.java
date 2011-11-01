//
// $Id$

package com.threerings.msoy.data;

import java.util.Set;

import javax.annotation.Generated;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.ObserverInfo;
import com.threerings.msoy.room.data.Track;

import static com.threerings.msoy.Log.log;

/**
 * Represents a connected msoy user.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberObject extends BodyObject
    implements MsoyUserObject, MsoyBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>actorState</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String ACTOR_STATE = "actorState";

    /** The field name of the <code>coins</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String COINS = "coins";

    /** The field name of the <code>bars</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BARS = "bars";

    /** The field name of the <code>level</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String LEVEL = "level";

    /** The field name of the <code>following</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String FOLLOWING = "following";

    /** The field name of the <code>followers</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String FOLLOWERS = "followers";

    /** The field name of the <code>tokens</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String TOKENS = "tokens";

    /** The field name of the <code>homeSceneId</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The field name of the <code>theme</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String THEME = "theme";

    /** The field name of the <code>avatar</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String AVATAR = "avatar";

    /** The field name of the <code>avatarCache</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String AVATAR_CACHE = "avatarCache";

    /** The field name of the <code>friends</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String FRIENDS = "friends";

    /** The field name of the <code>gateways</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String GATEWAYS = "gateways";

    /** The field name of the <code>imContacts</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String IM_CONTACTS = "imContacts";

    /** The field name of the <code>groups</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String GROUPS = "groups";

    /** The field name of the <code>newMailCount</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NEW_MAIL_COUNT = "newMailCount";

    /** The field name of the <code>game</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String GAME = "game";

    /** The field name of the <code>walkingId</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String WALKING_ID = "walkingId";

    /** The field name of the <code>headline</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HEADLINE = "headline";

    /** The field name of the <code>visitorInfo</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String VISITOR_INFO = "visitorInfo";

    /** The field name of the <code>onTour</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String ON_TOUR = "onTour";

    /** The field name of the <code>partyId</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PARTY_ID = "partyId";

    /** The field name of the <code>experiences</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String EXPERIENCES = "experiences";

    /** The field name of the <code>tracks</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String TRACKS = "tracks";
    // AUTO-GENERATED: FIELDS END

    /** A message sent by the server to denote a notification to be displayed.
     * Format: [ Notification ]. */
    public static final String NOTIFICATION = "notification";

    /** The ideal size of the avatar cache. */
    public static final int AVATAR_CACHE_SIZE = 5;

    /** The name and id information for this user. */
    public VizMemberName memberName;

    /** The current state of the body's actor, or null if unset/unknown/default. */
    public String actorState;

    /** How many coins we've got jangling around on our person. */
    public int coins;

    /** The number of bars the member has currently in their account. */
    public int bars;

    /** This user's current level. */
    public int level;

    /** The name of the member this member is following or null. */
    public MemberName following;

    /** The names of members following this member. */
    public DSet<MemberName> followers = DSet.newDSet();

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The id of the user's home scene. */
    public int homeSceneId;

    /** The definition of the theme this member is currently in, or null. */
    public GroupName theme;

    /** The avatar that the user has chosen, or null for guests. */
    public Avatar avatar;

    /** A cache of the user's 5 most recently touched avatars. */
    public DSet<Avatar> avatarCache;

    /** The online friends of this player. */
    public DSet<FriendEntry> friends = DSet.newDSet();

    /** The IM gateways available to this player. */
    public DSet<GatewayEntry> gateways = DSet.newDSet();

    /** The IM contacts of this player. */
    public DSet<ContactEntry> imContacts = DSet.newDSet();

    /** The groups of this player. */
    public DSet<GroupMembership> groups;

    /** A field that contains the number of unread messages in our mail inbox. */
    public int newMailCount;

    /** The game summary for the game that the player is lobbying for or currently playing. */
    public GameSummary game;

    /** If this member is currently walking a pet, the id of the pet being walked, else 0. */
    public int walkingId;

    /** The headline/status of this member. */
    public String headline;

    /** Player's tracking information. */
    public VisitorInfo visitorInfo;

    /** Whether this player is on the "whirled tour". We could also check whether touredRooms
     * is null, but that's not sent to the client. */
    public boolean onTour;

    /** The player's current partyId, or 0 if they're not in a party.
     * Used to signal the PartyDirector. */
    public int partyId;

    /** List of experiences this member has had recently. */
    public DSet<MemberExperience> experiences = DSet.newDSet();

    /** If this player is DJ-ing, the tracks they have queued up. */
    public DSet<Track> tracks = DSet.newDSet();

    public void initWithClient (MemberClientObject mcobj)
    {
        if (_mcobj != null) {
            throw new IllegalStateException("Already initialized!");
        }
        _mcobj = mcobj;
    }

    /**
     * Return true if this user is only viewing the scene and should not be rendered within it.
     */
    public boolean isViewer ()
    {
        return memberName.isViewer();
    }

    /**
     * Returns true if this user is a permaguest.
     */
    public boolean isPermaguest ()
    {
        return MemberMailUtil.isPermaguest(username.toString());
    }

    /**
     * Returns true if this member is away (from the keyboard... sort of), false if they are not.
     */
    public boolean isAway ()
    {
        return awayMessage != null; // message is set to non-null when we're away
    }

    /**
     * Return the client object connecting us to the user.
     */
    @Override
    public MemberClientObject getClientObject ()
    {
        return _mcobj;
    }

    // from MsoyBodyObject
    public BodyObject self ()
    {
        return this;
    }

    // from MsoyBodyObject
    public String getActorState ()
    {
        return actorState;
    }

    /**
     * Returns our home scene id or 1 (Brave New Whirled) if we have none.
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
     * Returns true if the specified member is our friend (and online). See MemberLocal for full
     * friend check.
     */
    public boolean isOnlineFriend (int memberId)
    {
        return friends.containsKey(memberId);
    }

    /**
     * Is this user a member of the specified group?
     */
    public boolean isGroupMember (int groupId)
    {
        return isGroupRank(groupId, Rank.MEMBER);
    }

    /**
     * Is this user a manager in the specified group?
     */
    public boolean isGroupManager (int groupId)
    {
        return isGroupRank(groupId, Rank.MANAGER);
    }

    /**
     * @return true if the user has at least the specified rank in the specified group.
     */
    public boolean isGroupRank (int groupId, Rank requiredRank)
    {
        return getGroupRank(groupId).compareTo(requiredRank) >= 0;
    }

    /**
     * Get the user's rank in the specified group.
     */
    public Rank getGroupRank (int groupId)
    {
        if (groups != null) {
            GroupMembership membInfo = groups.get(groupId);
            if (membInfo != null) {
                return membInfo.rank;
            }
        }
        return Rank.NON_MEMBER;
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
        return memberName.getId();
    }

    // from interface MsoyUserObject
    public void setParty (PartySummary summary)
    {
        _party = summary;
        int newPartyId = (summary == null) ? 0 : summary.id;
        if (newPartyId != partyId) {
            setPartyId(newPartyId); // avoid generating an extra event when we cross nodes
        }
    }

    // from interface MsoyUserObject
    public PartySummary getParty ()
    {
        return _party;
    }

    public boolean canEnterScene (
        int sceneId, int ownerId, byte ownerType, byte accessControl, Set<Integer> friendIds)
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
                hasRights = (getMemberId() == ownerId) ||
                   ((friendIds != null) && friendIds.contains(ownerId));
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

    @Override // from MsoyBodyObject
    public boolean isActor ()
    {
        return !isViewer();
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMemberName (VizMemberName value)
    {
        VizMemberName ovalue = this.memberName;
        requestAttributeChange(
            MEMBER_NAME, value, ovalue);
        this.memberName = value;
    }

    /**
     * Requests that the <code>actorState</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setActorState (String value)
    {
        String ovalue = this.actorState;
        requestAttributeChange(
            ACTOR_STATE, value, ovalue);
        this.actorState = value;
    }

    /**
     * Requests that the <code>coins</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setCoins (int value)
    {
        int ovalue = this.coins;
        requestAttributeChange(
            COINS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.coins = value;
    }

    /**
     * Requests that the <code>bars</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setLevel (int value)
    {
        int ovalue = this.level;
        requestAttributeChange(
            LEVEL, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.level = value;
    }

    /**
     * Requests that the <code>following</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToFollowers (MemberName elem)
    {
        requestEntryAdd(FOLLOWERS, followers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>followers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromFollowers (Comparable<?> key)
    {
        requestEntryRemove(FOLLOWERS, followers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>followers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setFollowers (DSet<MemberName> value)
    {
        requestAttributeChange(FOLLOWERS, value, this.followers);
        DSet<MemberName> clone = (value == null) ? null : value.clone();
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHomeSceneId (int value)
    {
        int ovalue = this.homeSceneId;
        requestAttributeChange(
            HOME_SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.homeSceneId = value;
    }

    /**
     * Requests that the <code>theme</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setTheme (GroupName value)
    {
        GroupName ovalue = this.theme;
        requestAttributeChange(
            THEME, value, ovalue);
        this.theme = value;
    }

    /**
     * Requests that the <code>avatar</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToAvatarCache (Avatar elem)
    {
        requestEntryAdd(AVATAR_CACHE, avatarCache, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>avatarCache</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromAvatarCache (Comparable<?> key)
    {
        requestEntryRemove(AVATAR_CACHE, avatarCache, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>avatarCache</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setAvatarCache (DSet<Avatar> value)
    {
        requestAttributeChange(AVATAR_CACHE, value, this.avatarCache);
        DSet<Avatar> clone = (value == null) ? null : value.clone();
        this.avatarCache = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>friends</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToFriends (FriendEntry elem)
    {
        requestEntryAdd(FRIENDS, friends, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>friends</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromFriends (Comparable<?> key)
    {
        requestEntryRemove(FRIENDS, friends, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>friends</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setFriends (DSet<FriendEntry> value)
    {
        requestAttributeChange(FRIENDS, value, this.friends);
        DSet<FriendEntry> clone = (value == null) ? null : value.clone();
        this.friends = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>gateways</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToGateways (GatewayEntry elem)
    {
        requestEntryAdd(GATEWAYS, gateways, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>gateways</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromGateways (Comparable<?> key)
    {
        requestEntryRemove(GATEWAYS, gateways, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>gateways</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setGateways (DSet<GatewayEntry> value)
    {
        requestAttributeChange(GATEWAYS, value, this.gateways);
        DSet<GatewayEntry> clone = (value == null) ? null : value.clone();
        this.gateways = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>imContacts</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToImContacts (ContactEntry elem)
    {
        requestEntryAdd(IM_CONTACTS, imContacts, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>imContacts</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromImContacts (Comparable<?> key)
    {
        requestEntryRemove(IM_CONTACTS, imContacts, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>imContacts</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setImContacts (DSet<ContactEntry> value)
    {
        requestAttributeChange(IM_CONTACTS, value, this.imContacts);
        DSet<ContactEntry> clone = (value == null) ? null : value.clone();
        this.imContacts = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>groups</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToGroups (GroupMembership elem)
    {
        requestEntryAdd(GROUPS, groups, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>groups</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromGroups (Comparable<?> key)
    {
        requestEntryRemove(GROUPS, groups, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>groups</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setGroups (DSet<GroupMembership> value)
    {
        requestAttributeChange(GROUPS, value, this.groups);
        DSet<GroupMembership> clone = (value == null) ? null : value.clone();
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setGame (GameSummary value)
    {
        GameSummary ovalue = this.game;
        requestAttributeChange(
            GAME, value, ovalue);
        this.game = value;
    }

    /**
     * Requests that the <code>walkingId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setOnTour (boolean value)
    {
        boolean ovalue = this.onTour;
        requestAttributeChange(
            ON_TOUR, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.onTour = value;
    }

    /**
     * Requests that the <code>partyId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPartyId (int value)
    {
        int ovalue = this.partyId;
        requestAttributeChange(
            PARTY_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.partyId = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>experiences</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToExperiences (MemberExperience elem)
    {
        requestEntryAdd(EXPERIENCES, experiences, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>experiences</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromExperiences (Comparable<?> key)
    {
        requestEntryRemove(EXPERIENCES, experiences, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>experiences</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setExperiences (DSet<MemberExperience> value)
    {
        requestAttributeChange(EXPERIENCES, value, this.experiences);
        DSet<MemberExperience> clone = (value == null) ? null : value.clone();
        this.experiences = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>tracks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToTracks (Track elem)
    {
        requestEntryAdd(TRACKS, tracks, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>tracks</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromTracks (Comparable<?> key)
    {
        requestEntryRemove(TRACKS, tracks, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>tracks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateTracks (Track elem)
    {
        requestEntryUpdate(TRACKS, tracks, elem);
    }

    /**
     * Requests that the <code>tracks</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setTracks (DSet<Track> value)
    {
        requestAttributeChange(TRACKS, value, this.tracks);
        DSet<Track> clone = (value == null) ? null : value.clone();
        this.tracks = clone;
    }
    // AUTO-GENERATED: METHODS END

    @Override // from BodyObject
    protected void addWhoData (StringBuilder buf)
    {
        buf.append("mid=").append(getMemberId()).append(" oid=");
        super.addWhoData(buf);
    }

    /** The user's party summary. Only needed on the server. */
    protected transient PartySummary _party;

    /** The actual {@link ClientObject} we represent. */
    protected transient MemberClientObject _mcobj;
}
