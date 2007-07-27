//
// $Id$

package com.threerings.msoy.game.data;

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
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public MemberName memberName;

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The avatar that the user has chosen, or null for guests. */
    public Avatar avatar;

    @Override // from BodyObject
    public TokenRing getTokens ()
    {
        return tokens;
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
    // AUTO-GENERATED: METHODS END
}
