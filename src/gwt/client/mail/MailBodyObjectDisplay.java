package client.mail;

import client.group.GroupInvite;
import client.person.FriendInvite;

import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailBodyObject;

/**
 * Represents the general object portion of a mail message body. This object is serialized
 * to and unserialized from persistent storage, and whenever it is to be displayed, the
 * relevant functions {@link #widgetForRecipient()} or {@link #widgetForOthers()) will be
 * called to retrieve the relevant UI.
 */
public abstract class MailBodyObjectDisplay
{
    /**
     * Constructs and retursn the appropriate {@link MailBodyObjectDisplay} for the given type,
     * and configured with the given state.
     */
    public static MailBodyObjectDisplay getDisplay (MailBodyObject object)
    {
        switch(object.type) {
        case MailBodyObject.TYPE_GROUP_INVITE:
            return new GroupInvite.Display(object.state);
        case MailBodyObject.TYPE_FRIEND_INVITE:
            return new FriendInvite.FriendBodyObject(object.state);
        }
        throw new IllegalArgumentException(
            "Unknown body object requested [type=" + object.type + "]");
    }

    /**
     * Returns the type of mail body object we represent.
     */
//    public abstract int getType ();

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
//    public abstract Map exportState ();

    /**
     *  Returns a {@link Widget} to display to anybody who is not this message's recipient.
     *  This object is meant to illustrate to an observer what the message looks like to
     *  the recipient, but any UI components it includes should be inactive. May be null,
     *  in which case nothing is displayed to the viewer.
     */
    public abstract Widget widgetForOthers (WebContext ctx);
}
