//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.data.GameSummary;

/**
 * Extends MemberInfo with world-specific information.
 */
public class WorldMemberInfo extends MemberInfo
    implements WorldOccupantInfo
{
    /** The state of the member's avatar. */
    public String state;

    /** The game summary for the user's currently pending game. */
    public GameSummary pendingGame;

//    /** The style of chat bubble to use. */
//    public short chatStyle;
//
//    /** The style with which the chat bubble pops up. */
//    public short chatPopStyle;

    /** Suitable for unserialization. */
    public WorldMemberInfo ()
    {
    }

    /**
     * Configures an occcupant info for the supplied member.
     */
    public WorldMemberInfo (MemberObject user)
    {
        super(user);

//        chatStyle = user.chatStyle;
//        chatPopStyle = user.chatPopStyle;

        if (user.avatar != null) {
            _media = user.avatar.avatarMedia;
        } else if (user.isGuest()) {
            _media = Avatar.getDefaultGuestAvatarMedia();
        } else {
            _media = Avatar.getDefaultMemberAvatarMedia();
        }

        state = user.avatarState;
        pendingGame = user.pendingGame;
    }

    // from interface WorldOccupantInfo
    public MediaDesc getMedia ()
    {
        return _media;
    }

    // from interface WorldOccupantInfo
    public String getState ()
    {
        return state;
    }

    protected MediaDesc _media;
}
