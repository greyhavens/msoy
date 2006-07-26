//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.IntListUtil;

import com.threerings.presents.dobj.DSet;

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
    /** The field name of the <code>memberId</code> field. */
    public static final String MEMBER_ID = "memberId";

    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>chatStyle</code> field. */
    public static final String CHAT_STYLE = "chatStyle";

    /** The field name of the <code>chatPopStyle</code> field. */
    public static final String CHAT_POP_STYLE = "chatPopStyle";

    /** The field name of the <code>friends</code> field. */
    public static final String FRIENDS = "friends";
    // AUTO-GENERATED: FIELDS END

    /** The memberId for this user, or 0 if they're not a member. */
    public int memberId;

    /** The scene id that the user is currently occupying. */
    public int sceneId;

    /** The object ID of the user's cluster. */
    public int clusterOid;

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The avatar that the user has chosen. */
    public MediaData avatar;

    /** The style of our chat. */
    public short chatStyle;

    /** The pop style of our chat. */
    public short chatPopStyle;

    /** The friends of this player. */
    public DSet<FriendEntry> friends = new DSet<FriendEntry>();

    /**
     * Return true if this user is merely a guest.
     */
    public boolean isGuest ()
    {
        // TODO
        return username.toString().startsWith("guest");
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

    @Override
    public TokenRing getTokens ()
    {
        return tokens;
    }

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

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>memberId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMemberId (int value)
    {
        int ovalue = this.memberId;
        requestAttributeChange(
            MEMBER_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.memberId = value;
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
        this.friends = (value == null) ? null : value.typedClone();
    }
    // AUTO-GENERATED: METHODS END

    // TEMP: media ids for our standard avatars
    public static final int[] AVATARS = {
        0, 1, 2, 20, 21, 22, 25, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52
    };
}
