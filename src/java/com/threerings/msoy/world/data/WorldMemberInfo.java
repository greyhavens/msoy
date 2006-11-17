//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Extends MemberInfo with world-specific information.
 */
public class WorldMemberInfo extends MemberInfo
{
    /** The media that represents our avatar. */
    public MediaDesc avatar;

    /** The style of chat bubble to use. */
    public short chatStyle;

    /** The style with which the chat bubble pops up. */
    public short chatPopStyle;

    /** Suitable for unserialization. */
    public WorldMemberInfo ()
    {
    }

    public WorldMemberInfo (MemberObject user)
    {
        super(user);

        if (user.avatar != null) {
            avatar = user.avatar.avatarMedia;

        } else if (user.isGuest()) {
            avatar = GUEST_AVATAR;

        } else {
            avatar = DEFAULT_AVATAR;
        }

        chatStyle = user.chatStyle;
        chatPopStyle = user.chatPopStyle;
    }

    /** The default avatar to use for guests. */
    // TODO: how will these defaults actually be specified?
    protected static MediaDesc GUEST_AVATAR = new MediaDesc(
        "4890c3d1e6c16a62746f62cf7d854ed14406a78b.swf");

    /** The default avatar to use for unconfigured members. */
    // TODO: how will these defaults actually be specified?
    protected static MediaDesc DEFAULT_AVATAR = new MediaDesc(
        "0738674d2c3dab04978861f98a82ddc5e7b56e1b.swf");
}
