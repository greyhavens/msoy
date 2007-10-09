//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

import com.whirled.data.GameData;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on a player logged on to an MSOY Game server.
 */
public class PlayerObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";

    /** The field name of the <code>tokens</code> field. */
    public static const TOKENS :String = "tokens";

    /** The field name of the <code>avatar</code> field. */
    public static const AVATAR :String = "avatar";

    /** The field name of the <code>humanity</code> field. */
    public static const HUMANITY :String = "humanity";

    /** The field name of the <code>gameState</code> field. */
    public static const GAME_STATE :String = "gameState";

    /** The field name of the <code>questState</code> field. */
    public static const QUEST_STATE :String = "questState";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public var memberName :MemberName;

    /** The tokens defining the access controls for this user. */
    public var tokens :MsoyTokenRing;

    /** The avatar that the user has chosen, or null for guests. */
    public var avatar :Avatar;

    /** Our current assessment of how likely to be human this member is, in [0, 255]. */
    public var humanity :int;

    /** Game state entries for the world game we're currently on. */
    public var gameState :DSet;

    /** The quests of our current world game that we're currently on. */
    public var questState :DSet;

    /** Contains information on player's ownership of game content (populated lazily). */
    public var gameContent :DSet;

    // from BodyObject
    override public function getTokens () :TokenRing
    {
        return tokens;
    }

    // from BodyObject
    override public function getVisibleName () :Name
    {
        return memberName;
    }

    /**
     * Returns this member's unique id.
     */
    public function getMemberId () :int
    {
        return (memberName == null) ? MemberName.GUEST_ID : memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return (getMemberId() == MemberName.GUEST_ID);
    }

    /**
     * Get the media to use as our headshot.
     */
    public function getHeadShotMedia () :MediaDesc
    {
        if (avatar != null) {
            return avatar.getThumbnailMedia();
        }
        return Item.getDefaultThumbnailMediaFor(Item.AVATAR);
    }

    /**
     * Return our assessment of how likely this member is to be human, in [0, 1].
     */
    public function getHumanity () :Number
    {
        return humanity / 255;
    }

    /**
     * Returns true if content is resolved for the specified game, false if it is not yet ready.
     */
    public function isContentResolved (gameId :int) :Boolean
    {
        return ownsGameContent(gameId, GameData.RESOLVED_MARKER, "");
    }

    /**
     * Returns true if this player owns the specified piece of game content. <em>Note:</em> the
     * content must have previously been resolved, which happens when the player enters the game in
     * question.
     */
    public function ownsGameContent (gameId :int, type :int, ident :String) :Boolean
    {
        var key :GameContentOwnership = new GameContentOwnership();
        key.gameId = gameId;
        key.type = type;
        key.ident = ident;
        return gameContent.containsKey(key);
    }

    // from BodyObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberName = (ins.readObject() as MemberName);
        tokens = (ins.readObject() as MsoyTokenRing);
        avatar = (ins.readObject() as Avatar);
        humanity = ins.readInt();
        gameState = (ins.readObject() as DSet);
        questState = (ins.readObject() as DSet);
        gameContent = (ins.readObject() as DSet);
    }
}
}
