//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.data.TokenRing;

import com.whirled.game.data.GameData;
import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;


/**
 * Contains information on a player logged on to an MSOY Game server.
 */
public class PlayerObject extends BodyObject
    implements MsoyUserObject
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

    /** The field name of the <code>gameState</code> field. */
    public static final String GAME_STATE = "gameState";

    /** The field name of the <code>questState</code> field. */
    public static final String QUEST_STATE = "questState";

    /** The field name of the <code>gameContent</code> field. */
    public static final String GAME_CONTENT = "gameContent";

    /** The field name of the <code>referral</code> field. */
    public static final String REFERRAL = "referral";
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

    /** Game state entries for the world game we're currently on. */
    public DSet<GameState> gameState = new DSet<GameState>();

    /** The quests of our current world game that we're currently on. */
    public DSet<QuestState> questState = new DSet<QuestState>();

    /** Contains information on player's ownership of game content (populated lazily). */
    public DSet<GameContentOwnership> gameContent = new DSet<GameContentOwnership>();

    /** Player's referral information. */
    public ReferralInfo referral;

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

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new WhirledGameOccupantInfo(this);
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
    public void setFriends (DSet<FriendEntry> value)
    {
        requestAttributeChange(FRIENDS, value, this.friends);
        @SuppressWarnings("unchecked") DSet<FriendEntry> clone =
            (value == null) ? null : value.typedClone();
        this.friends = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>gameState</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGameState (GameState elem)
    {
        requestEntryAdd(GAME_STATE, gameState, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>gameState</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGameState (Comparable key)
    {
        requestEntryRemove(GAME_STATE, gameState, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>gameState</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGameState (GameState elem)
    {
        requestEntryUpdate(GAME_STATE, gameState, elem);
    }

    /**
     * Requests that the <code>gameState</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGameState (DSet<GameState> value)
    {
        requestAttributeChange(GAME_STATE, value, this.gameState);
        @SuppressWarnings("unchecked") DSet<GameState> clone =
            (value == null) ? null : value.typedClone();
        this.gameState = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>questState</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToQuestState (QuestState elem)
    {
        requestEntryAdd(QUEST_STATE, questState, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>questState</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromQuestState (Comparable key)
    {
        requestEntryRemove(QUEST_STATE, questState, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>questState</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateQuestState (QuestState elem)
    {
        requestEntryUpdate(QUEST_STATE, questState, elem);
    }

    /**
     * Requests that the <code>questState</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setQuestState (DSet<QuestState> value)
    {
        requestAttributeChange(QUEST_STATE, value, this.questState);
        @SuppressWarnings("unchecked") DSet<QuestState> clone =
            (value == null) ? null : value.typedClone();
        this.questState = clone;
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
    public void removeFromGameContent (Comparable key)
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
        @SuppressWarnings("unchecked") DSet<GameContentOwnership> clone =
            (value == null) ? null : value.typedClone();
        this.gameContent = clone;
    }

    /**
     * Requests that the <code>referral</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setReferral (ReferralInfo value)
    {
        ReferralInfo ovalue = this.referral;
        requestAttributeChange(
            REFERRAL, value, ovalue);
        this.referral = value;
    }
    // AUTO-GENERATED: METHODS END

    @Override // from BodyObject
    protected void addWhoData (StringBuilder buf)
    {
        buf.append("id=").append(getMemberId()).append(" oid=");
        super.addWhoData(buf);
    }
}
