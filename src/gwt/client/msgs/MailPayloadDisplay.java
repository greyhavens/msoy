//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MailMessage;

/**
 * Base class for payload visualizers. Concrete subclasses of this object are configured with a
 * {@link MailMessage}, and will be asked to hand out Widgets to be displayed in mail messages in
 * the GTW Mail system through the functions {@link #widgetForRecipient()} and {@link
 * #widgetForOthers()).
 */
public abstract class MailPayloadDisplay
{
    /**
     * Constructs and retursn the appropriate {@link MailPayloadDisplay} for the
     * given mail message (presuming it has a payload).
     */
    public static MailPayloadDisplay getDisplay (MailMessage message)
    {
        if (message.payload == null) {
            return null;
        }
        switch (message.payload.getType()) {
        case MailPayload.TYPE_GROUP_INVITE:
            return new GroupInvite.Display(message);
        case MailPayload.TYPE_FRIEND_INVITE:
            return new FriendInvite.Display(message);
        case MailPayload.TYPE_ITEM_GIFT:
            return new ItemGift.Display(message);
        case MailPayload.TYPE_TROPHY_AWARD:
            return new TrophyAward.Display(message);
        }
        throw new IllegalArgumentException(
            "Unknown payload requested [type=" + message.payload.getType() + "]");
    }

    public MailPayloadDisplay (MailMessage message)
    {
        _message = message;
    }

    /**
     * Returns the {@link Widget} to be displayed to the recipient of this message.  This object
     * may (and typically will) contain active UI components to initiate requests to the
     * server. May be null, in which case nothing is displayed to the recipient.
     */
    public abstract Widget widgetForRecipient (MailUpdateListener listener);

    /**
     * Returns a {@link Widget} to display to anybody who is not this message's recipient.  This
     * object is meant to illustrate to an observer what the message looks like to the recipient,
     * but any UI components it includes should be inactive. May be null, in which case nothing is
     * displayed to the viewer.
     */
    public abstract Widget widgetForOthers ();

    /**
     * Asks this if the payload represented by this display widget may be deleted, in which
     * case we return null. If it may not, return a string with the human-readable reason;
     * it will be displayed to the user.
     *
     * TODO: Ideally this should be some kind of delete-button-outgraying callback mechanism,
     * but that may be over the top.
     */
    public abstract String okToDelete ();

    /**
     * Performs a server request to update the state for this message. If the callback
     * argument is null, one is created for you which does nothing on success and throws
     * a RuntimeException on failure.
     */
    protected void updateState (MailPayload payload, AsyncCallback callback)
    {
        if (callback == null) {
            callback = new AsyncCallback() {
                public void onSuccess (Object result) {
                }
                public void onFailure (Throwable caught) {
                    throw new RuntimeException(caught);
                }
            };
        }
        CMsgs.mailsvc.updatePayload(CMsgs.ident, _message.headers.folderId,
                                    _message.headers.messageId, payload, callback);
    }

    protected MailMessage _message;
}
