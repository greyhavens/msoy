//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Extends basic OccupantInfo with member-specific information.
 */
public class MemberInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public MediaDesc media;

    /** The itemId of the item that is the avatar. */
    public int avatarId;

    /** The style of chat bubble to use. */
    public short chatStyle;

    /** The style with which the chat bubble pops up. */
    public short chatPopStyle;

    /** Suitable for unserialization. */
    public MemberInfo ()
    {
    }

    public MemberInfo (MemberObject user)
    {
        super(user);

        if (user.avatar != null) {
            avatarId = user.avatar.itemId;
            media = user.avatar.avatarMedia;

        } else {
            avatarId = 0;
            // TODO: how will these defaults actually be specified?
            if (user.isGuest()) {
                // guest avatar
                media = new MediaDesc(
                    "4890c3d1e6c16a62746f62cf7d854ed14406a78b.swf");
            } else {
                // default avatar
                media = new MediaDesc(
                    "0738674d2c3dab04978861f98a82ddc5e7b56e1b.swf");
            }
        }

        chatStyle = user.chatStyle;
        chatPopStyle = user.chatPopStyle;
    }

    /**
     * Get the member id for this user, or 0 if they're a guest.
     */
    public int getMemberId ()
    {
        return ((MemberName) username).getMemberId();
    }
}
