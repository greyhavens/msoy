package com.threerings.msoy.web.data;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.io.Streamable;
import com.threerings.msoy.web.client.WebContext;

/**
 * Represents the general object portion of a mail message body. This object is serialized
 * to and unserialized from persistent storage, and whenever it is to be displayed, the
 * relevant functions {@link #widgetForRecipient()} or {@link #widgetForOthers()) will be
 * called to retrieve the relevant UI.
 */
public abstract class MailBodyObject
    implements IsSerializable, Streamable
{
    /**
     * The identifying integer of a {@link GroupInvite} mail body object. 
     */
    public static final int TYPE_GROUP_INVITE = 1;

    /**
     * The identifying integer of a {@link FriendInvite} mail body object. 
     */
    public static final int TYPE_FRIEND_INVITE = 2;

    /**
     * Constructs and retursn the appropriate {@link MailBodyObject} for the given type,
     * and configured with the given state.
     */
    public static MailBodyObject buildBodyObject (int type, Map state)
    {
        switch(type) {
        case TYPE_GROUP_INVITE:
            return new GroupInvite.GroupBodyObject(state);
        case TYPE_FRIEND_INVITE:
            return new FriendInvite.FriendBodyObject(state);
        }
        throw new IllegalArgumentException("Unknown body object requested [type=" + type + "]");
    }


    /**
     * Returns the type of mail body object we represent.
     */
    public abstract int getType ();

    /**
     *  Returns the {@link Widget} to be displayed to the recipient of this message.
     *  This object may (and typically will) contain active UI components to initiate
     *  requests to the server. May be null, in which case nothing is displayed to
     *  the recipient.
     */
    public abstract Widget widgetForRecipient (WebContext ctx);

    /**
     * Exports the state of this object required to reconstruct it in {@link #buildBodyObject}.
     */
    public abstract Map exportState ();

    /**
     *  Returns a {@link Widget} to display to anybody who is not this message's recipient.
     *  This object is meant to illustrate to an observer what the message looks like to
     *  the recipient, but any UI components it includes should be inactive. May be null,
     *  in which case nothing is displayed to the viewer.
     */
    public abstract Widget widgetForOthers (WebContext ctx);

    /**
     * Implements {@link Object#hashCode()}.
     */
    public abstract int hashCode ();

    /**
     * Implements {@link Object#equals(Object)}.
     */
    public abstract boolean equals (Object obj);
}
