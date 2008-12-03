//
// $Id$

package com.threerings.msoy.game.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

import com.threerings.crowd.data.TokenRing;

import com.whirled.game.data.GameData;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledPlayerObject;
import com.whirled.game.server.PropertySpaceHelper;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;

import static com.threerings.msoy.Log.log;

/**
 * Contains information on a player logged on to an MSOY Game server.
 */
public class PlayerObject extends WhirledPlayerObject
    implements MsoyUserObject, PropertySpaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>humanity</code> field. */
    public static final String HUMANITY = "humanity";

    /** The field name of the <code>friends</code> field. */
    public static final String FRIENDS = "friends";

    /** The field name of the <code>gameContent</code> field. */
    public static final String GAME_CONTENT = "gameContent";

    /** The field name of the <code>visitorInfo</code> field. */
    public static final String VISITOR_INFO = "visitorInfo";

    /** The field name of the <code>propertyService</code> field. */
    public static final String PROPERTY_SERVICE = "propertyService";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public VizMemberName memberName;

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** Our current assessment of how likely to be human this member is, in [0, {@link
     * MsoyCodes#MAX_HUMANITY}]. */
    public int humanity;

    /** A snapshot of this players friends loaded when they logged onto the game server. Online
     * status is not filled in and this set is *not* updated if friendship is made or broken during
     * a game. */
    public DSet<FriendEntry> friends;

    /** Contains information on player's ownership of game content (populated lazily). */
    public DSet<GameContentOwnership> gameContent = new DSet<GameContentOwnership>();

    /** Player's referral information. */
    public VisitorInfo visitorInfo;

    /** Service for setting player properties. */
    public PropertySpaceMarshaller propertyService;

    /**
     * Return true if this user is a guest.
     */
    public boolean isGuest ()
    {
        return memberName.isGuest();
    }

    /**
     * Get the media to use as our headshot.
     */
    public MediaDesc getHeadShotMedia ()
    {
        return memberName.getPhoto();
    }

    /**
     * Returns true if content is resolved for the specified game, false if it is not yet ready.
     */
    public boolean isContentResolved (int gameId)
    {
        return ownsGameContent(gameId, GameData.RESOLVED_MARKER, "");
    }

    /**
     * Returns true if this player owns the specified piece of game content. <em>Note:</em> the
     * content must have previously been resolved, which happens when the player enters the game in
     * question.
     */
    public boolean ownsGameContent (int gameId, byte type, String ident)
    {
        GameContentOwnership key = new GameContentOwnership();
        key.gameId = gameId;
        key.type = type;
        key.ident = ident;
        return gameContent.containsKey(key);
    }

    /**
     * Returns {@link #visitorInfo}.id but logs a warning and stack trace if visitorInfo is null.
     */
    public String getVisitorId ()
    {
        if (visitorInfo == null) {
            log.warning("Member missing visitorInfo", "who", who(), new Exception());
            return "";
        }
        return visitorInfo.id;
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

    // from interface MsoyUserObject
    public float getHumanity ()
    {
        return humanity / (float)MsoyCodes.MAX_HUMANITY;
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

    // from PropertySpaceObject
    public Map<String, Object> getUserProps ()
    {
        return _props;
    }

    // from PropertySpaceObject
    public Set<String> getDirtyProps ()
    {
        return _dirty;
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
     * <code>gameContent</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGameContent (GameContentOwnership elem)
    {
        requestEntryAdd(GAME_CONTENT, gameContent, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>gameContent</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGameContent (Comparable<?> key)
    {
        requestEntryRemove(GAME_CONTENT, gameContent, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>gameContent</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGameContent (GameContentOwnership elem)
    {
        requestEntryUpdate(GAME_CONTENT, gameContent, elem);
    }

    /**
     * Requests that the <code>gameContent</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGameContent (DSet<GameContentOwnership> value)
    {
        requestAttributeChange(GAME_CONTENT, value, this.gameContent);
        DSet<GameContentOwnership> clone = (value == null) ? null : value.typedClone();
        this.gameContent = clone;
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
     * Requests that the <code>propertyService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPropertyService (PropertySpaceMarshaller value)
    {
        PropertySpaceMarshaller ovalue = this.propertyService;
        requestAttributeChange(
            PROPERTY_SERVICE, value, ovalue);
        this.propertyService = value;
    }
    // AUTO-GENERATED: METHODS END

    @Override // from BodyObject
    protected void addWhoData (StringBuilder buf)
    {
        buf.append("id=").append(getMemberId()).append(" oid=");
        super.addWhoData(buf);
    }

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        PropertySpaceHelper.writeProperties(this, out);
    }

    /**
     * A custom serialization method.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        PropertySpaceHelper.readProperties(this, ins);
    }


    /**
     * The current state of game data.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected transient HashMap<String, Object> _props = new HashMap<String, Object>();

    /**
     * The persistent properties that have been written to since startup.
     */
    protected transient Set<String> _dirty = new HashSet<String>();
}
