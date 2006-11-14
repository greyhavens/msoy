//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents the general object portion of a mail message body. This object is serialized
 * to and unserialized from persistent storage through its 'type' and 'state' members, and
 * whenever it is to be displayed, the relevant functions {@link #widgetForRecipient()} or
 * {@link #widgetForOthers()) will be called to retrieve the relevant UI.
 */
public class MailBodyObject
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

    public int type;
    public Map state;

    public MailBodyObject ()
    {
    }

    public MailBodyObject (int type, Map state)
    {
        this.type = type;
        this.state = state;
    }

    // @Override
    public int hashCode ()
    {
        return type + (state != null ? 31*state.hashCode() : 0);
    }
    
    // @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MailFolder)) {
            return false;
        }
        MailBodyObject other = (MailBodyObject) obj;
        if (type != other.type) {
            return false;
        }
        if (state == null) {
            return other.state == null;
        }
        return state.equals(other.state);
    }
}
