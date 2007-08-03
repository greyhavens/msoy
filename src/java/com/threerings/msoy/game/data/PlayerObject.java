//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.msoy.data.MsoyCodes;
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
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>humanity</code> field. */
    public static final String HUMANITY = "humanity";
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

    /**
     * Returns this member's unique id.
     */
    public int getMemberId ()
    {
        return (memberName == null) ? MemberName.GUEST_ID : memberName.getMemberId();
    }

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
     * Return our assessment of how likely this member is to be human, in [0, 1).
     */
    public float getHumanity ()
    {
        return humanity / (float)MsoyCodes.MAX_HUMANITY;
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
    // AUTO-GENERATED: METHODS END
}
