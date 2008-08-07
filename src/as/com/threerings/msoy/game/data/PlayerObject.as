//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

import com.whirled.game.data.GameData;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.item.data.all.Item;

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

    /** The field name of the <code>photo</code> field. */
    public static const PHOTO :String = "photo";

    /** The field name of the <code>humanity</code> field. */
    public static const HUMANITY :String = "humanity";

    /** The field name of the <code>questState</code> field. */
    public static const QUEST_STATE :String = "questState";

    /** The field name of the <code>referral</code> field. */
    public static const REFERRAL :String = "referral";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public var memberName :VizMemberName;

    /** The tokens defining the access controls for this user. */
    public var tokens :MsoyTokenRing;

    /** Our current assessment of how likely to be human this member is, in [0, 255]. */
    public var humanity :int;

    /** A snapshot of this players friends loaded when they logged onto the game server. Online
     * status is not filled in and this set is *not* updated if friendship is made or broken during
     * a game. */
    public var friends :DSet /* FriendEntry */;
    FriendEntry; // reference to force compilation

    /** The quests of our current world game that we're currently on. */
    public var questState :DSet;

    /** Contains information on player's ownership of game content (populated lazily). */
    public var gameContent :DSet;

    /** Player's referral information. */
    public var referral :ReferralInfo;

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
        return memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return MemberName.isGuest(getMemberId());
    }

    /**
     * Get the media to use as our headshot.
     */
    public function getHeadShotMedia () :MediaDesc
    {
        return memberName.getPhoto();
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

        memberName = (ins.readObject() as VizMemberName);
        tokens = (ins.readObject() as MsoyTokenRing);
        humanity = ins.readInt();
        friends = (ins.readObject() as DSet);
        questState = (ins.readObject() as DSet);
        gameContent = (ins.readObject() as DSet);
        referral = (ins.readObject() as ReferralInfo);
    }
}
}
