//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Represents the general object portion of a mail message body. This object is serialized to and
 * unserialized from persistent storage.
 */
public abstract class MailPayload
    implements IsSerializable, Streamable
{
    /**
     * The identifying integer of a GroupInvitePayload.
     */
    public static final int TYPE_GROUP_INVITE = 1;

    /**
     * The identifying integer of a FriendInvitePayload.
     */
    public static final int TYPE_FRIEND_INVITE = 2;

    /**
     * A no longer used identifier.
     */
    public static final int UNUSED = 3;

    /**
     * The identifying integer of a GameAwardPayload.
     *
     * Note: I am not sure this was ever used, and if it was, it no longer is. There is not
     * a single conversation record of type 4 in the production database.
     */
    public static final int UNUSED_TYPE_GAME_AWARD = 4;

    /**
     * The identifying integer of a PresentPayload.
     */
    public static final int TYPE_PRESENT = 5;

    /**
     * The identifying integer of a GameInvitePayload.
     */
    public static final int TYPE_GAME_INVITE = 6;

    /**
     * The id for a RoomGiftPayload.
     */
    public static final int TYPE_ROOM_GIFT = 7;

    /**
     * Returns the type associated with this payload instance.
     */
    public abstract int getType ();
}
