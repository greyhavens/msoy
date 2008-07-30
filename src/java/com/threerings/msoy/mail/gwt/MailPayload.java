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
     * The identifying integer of a ItemGiftPayload.
     */
    public static final int TYPE_ITEM_GIFT = 3;

    /**
     * The identifying integer of a GameAwardPayload.
     */
    public static final int TYPE_GAME_AWARD = 4;

    /**
     * The identifying integer of a PresentPayload.
     */
    public static final int TYPE_PRESENT = 5;

    /**
     * Returns the type associated with this payload instance.
     */
    public abstract int getType ();
}
