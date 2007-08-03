//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

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
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public var memberName :MemberName;

    /** The tokens defining the access controls for this user. */
    public var tokens :MsoyTokenRing;

    /** The avatar that the user has chosen, or null for guests. */
    public var avatar :Avatar;

    /** Our current assessment of how likely to be human this member is, in [0, 255]. */
    public var humanity :int;

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

    // from BodyObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberName = (ins.readObject() as MemberName);
        tokens = (ins.readObject() as MsoyTokenRing);
        avatar = (ins.readObject() as Avatar);
        humanity = ins.readInt();
    }
}
}
