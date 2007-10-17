//
// $Id$

package com.threerings.msoy.person.data;


import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents the general object portion of a mail message body. This object is serialized
 * to and unserialized from persistent storage through its 'type' and 'state' members, and
 * whenever it is to be displayed, the relevant functions {@link #widgetForRecipient()} or
 * {@link #widgetForOthers()) will be called to retrieve the relevant UI.
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
     * The identifying integer of a {@link TrophyAwardPayload} payload.
     */
    public static final int TYPE_TROPHY_AWARD = 4;

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
        case TYPE_TROPHY_AWARD:
            return TrophyAwardPayload.class;
        }
        throw new IllegalArgumentException("Unknown payload [type= " + type + "]");
    }

    /**
     * Returns the type associated with this payload instance.
     */
    public abstract int getType ();
}
