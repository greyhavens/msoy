//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.IntListUtil;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.TokenRing;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";

    /** The field name of the <code>recentScenes</code> field. */
    public static final String RECENT_SCENES = "recentScenes";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>homeSceneId</code> field. */
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>chatStyle</code> field. */
    public static final String CHAT_STYLE = "chatStyle";

    /** The field name of the <code>chatPopStyle</code> field. */
    public static final String CHAT_POP_STYLE = "chatPopStyle";

    /** The field name of the <code>friends</code> field. */
    public static final String FRIENDS = "friends";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public MemberName memberName;

    /** The scene id that the user is currently occupying. */
    public int sceneId;

    /** The object ID of the user's cluster. */
    public int clusterOid;

    /** The recent scenes we've been through. */
    public DSet<SceneBookmarkEntry> recentScenes =
        new DSet<SceneBookmarkEntry>();

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The id of the user's home scene. */
    public int homeSceneId;

    /** The avatar that the user has chosen. */
    public MediaData avatar;

    /** The style of our chat. */
    public short chatStyle;

    /** The pop style of our chat. */
    public short chatPopStyle;

    /** The friends of this player. */
    public DSet<FriendEntry> friends = new DSet<FriendEntry>();

    /**
     * Returns this member's unique id.
     */
    public int getMemberId ()
    {
        return (memberName == null) ? 0 : memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public boolean isGuest ()
    {
        return (getMemberId() <= 0);
    }

    // documentation inherited from superinterface ScenedBodyObject
    public int getSceneId ()
    {
        return sceneId;
    }

    // documentation inherited from interface ClusteredBodyObject
    public int getClusterOid ()
    {
        return clusterOid;
    }

    // documentation inherited from interface ClusteredBodyObject
    public String getClusterField ()
    {
        return CLUSTER_OID;
    }

    // documentation inherited
    public OccupantInfo createOccupantInfo ()
    {
        return new MemberInfo(this);
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

    // TEMP: hackery
    @Override
    public void setOid (int oid)
    {
        super.setOid(oid);

        // configure some starter options
        avatar = new MediaData(AVATARS[oid % AVATARS.length]);
        if (avatar.id == 0) {
            chatStyle = (short) 1;
        }
        chatPopStyle = (short) (oid % 2);
    }
    // END

    public void alter (String field)
    {
        if (CHAT_STYLE.equals(field)) {
            setChatStyle((short) ((chatStyle + 1) % 2));

        } else if (CHAT_POP_STYLE.equals(field)) {
            setChatPopStyle((short) ((chatPopStyle + 1) % 2));

        } else {
            int increment = AVATAR.equals(field) ? 1 : (AVATARS.length - 1);
            int dex = IntListUtil.indexOf(AVATARS, avatar.id);
            int newId = AVATARS[(dex + increment) % AVATARS.length];
            setAvatar(new MediaData(newId));
        }
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
                newEntry.orderingId = sbe.orderingId;
                updateRecentScenes(newEntry);
                return;
            }
            if (oldest == null || oldest.lastVisit > sbe.lastVisit) {
                oldest = sbe;
            }
        }

        int size = recentScenes.size();
        if (size < MAX_RECENT_SCENES) {
            newEntry.orderingId = (short) size;
            addToRecentScenes(newEntry);

        } else {
            newEntry.orderingId = oldest.orderingId;
            updateRecentScenes(newEntry);
        }
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
     * Requests that the <code>sceneId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSceneId (int value)
    {
        int ovalue = this.sceneId;
        requestAttributeChange(
            SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.sceneId = value;
    }

    /**
     * Requests that the <code>clusterOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setClusterOid (int value)
    {
        int ovalue = this.clusterOid;
        requestAttributeChange(
            CLUSTER_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.clusterOid = value;
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
    public void setRecentScenes (DSet<com.threerings.msoy.data.SceneBookmarkEntry> value)
    {
        requestAttributeChange(RECENT_SCENES, value, this.recentScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.SceneBookmarkEntry> clone =
            (value == null) ? null : value.typedClone();
        this.recentScenes = clone;
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
    public void setAvatar (MediaData value)
    {
        MediaData ovalue = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = value;
    }

    /**
     * Requests that the <code>chatStyle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setChatStyle (short value)
    {
        short ovalue = this.chatStyle;
        requestAttributeChange(
            CHAT_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
        this.chatStyle = value;
    }

    /**
     * Requests that the <code>chatPopStyle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setChatPopStyle (short value)
    {
        short ovalue = this.chatPopStyle;
        requestAttributeChange(
            CHAT_POP_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
        this.chatPopStyle = value;
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
    public void setFriends (DSet<com.threerings.msoy.data.FriendEntry> value)
    {
        requestAttributeChange(FRIENDS, value, this.friends);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.FriendEntry> clone =
            (value == null) ? null : value.typedClone();
        this.friends = clone;
    }
    // AUTO-GENERATED: METHODS END

    // TEMP: media ids for our standard avatars
    public static final int[] AVATARS = {
        0, 1, 2, 20, 21, 22, 25, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54
    };

    public static final int MAX_RECENT_SCENES = 10;
}
