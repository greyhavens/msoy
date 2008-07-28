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
     * The identifying integer of a {@link GroupInvitePayload} payload.
     */
    public static final int TYPE_GROUP_INVITE = 1;

    /**
     * The identifying integer of a {@link FriendInvitePayload} payload.
     */
    public static final int TYPE_FRIEND_INVITE = 2;

    /**
     * The identifying integer of a {@link ItemGiftPayload} payload.
     */
    public static final int TYPE_ITEM_GIFT = 3;

    /**
     * The identifying integer of a {@link GameAwardPayload} payload.
     */
    public static final int TYPE_GAME_AWARD = 4;

    /**
     * The identifying integer of a {@link PresentPayload} payload.
     */
    public static final int TYPE_PRESENT = 5;

    /**
     * Returns the class registered for the specified payload type.
     *
     * @exception IllegalArgumentException thrown if an unknown payload type is provided.
     */
    public static Class getPayloadClass (int type)
    {
        switch(type) {
        case TYPE_GROUP_INVITE:
            return GroupInvitePayload.class;
        case TYPE_FRIEND_INVITE:
            return FriendInvitePayload.class;
        case TYPE_ITEM_GIFT:
            return ItemGiftPayload.class;
        case TYPE_GAME_AWARD:
            return GameAwardPayload.class;
        case TYPE_PRESENT:
            return PresentPayload.class;
        }
        throw new IllegalArgumentException("Unknown payload [type= " + type + "]");
    }

    /**
     * Returns the type associated with this payload instance.
     */
    public abstract int getType ();
}
