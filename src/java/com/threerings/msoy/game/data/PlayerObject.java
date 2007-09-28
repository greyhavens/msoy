//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

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

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>humanity</code> field. */
    public static final String HUMANITY = "humanity";

    /** The field name of the <code>gameState</code> field. */
    public static final String GAME_STATE = "gameState";

    /** The field name of the <code>questState</code> field. */
    public static final String QUEST_STATE = "questState";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public MemberName memberName;

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The avatar that the user has chosen, or null for guests. */
    public Avatar avatar;

    /** Our current assessment of how likely to be human this member is, in [0, {@link
     * MsoyCodes#MAX_HUMANITY}]. */
    public int humanity;

    /** Game state entries for the world game we're currently on. */
    public DSet<GameState> gameState = new DSet<GameState>();

    /** The quests of our current world game that we're currently on. */
    public DSet<QuestState> questState = new DSet<QuestState>();

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
    public void setAvatar (Avatar value)
    {
        Avatar ovalue = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = value;
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
    public void setGameState (DSet<com.threerings.msoy.game.data.GameState> value)
    {
        requestAttributeChange(GAME_STATE, value, this.gameState);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.game.data.GameState> clone =
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
    public void setQuestState (DSet<com.threerings.msoy.game.data.QuestState> value)
    {
        requestAttributeChange(QUEST_STATE, value, this.questState);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.game.data.QuestState> clone =
            (value == null) ? null : value.typedClone();
        this.questState = clone;
    }
    // AUTO-GENERATED: METHODS END
}
