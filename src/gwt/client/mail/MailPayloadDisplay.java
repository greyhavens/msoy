//
// $Id$

package client.mail;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.person.gwt.ConvMessage;
import com.threerings.msoy.person.gwt.MailPayload;

/**
 * Base class for payload visualizers. Concrete subclasses of this object are configured with a
 * {@link ConvMessage}, and will be asked to create displays for mail payloads via {@link
 * #widgetForRecipient()} and {@link #widgetForSender()).
 */
public class MailPayloadDisplay
{
    /**
     * Constructs and retursn the appropriate {@link MailPayloadDisplay} for the given mail message
     * (presuming it has a payload).
     */
    public static MailPayloadDisplay getDisplay (int convoId, ConvMessage message)
    {
        if (message.payload == null) {
            return null;
        }

        MailPayloadDisplay display;
        switch (message.payload.getType()) {
        case MailPayload.TYPE_GROUP_INVITE:
            display = new GroupInviteDisplay();
            break;
        case MailPayload.TYPE_FRIEND_INVITE:
            display = new FriendInviteDisplay();
            break;
        case MailPayload.TYPE_ITEM_GIFT:
            display = new ItemGiftDisplay();
            break;
        case MailPayload.TYPE_GAME_AWARD:
            display = new GameAwardDisplay();
            break;
        case MailPayload.TYPE_PRESENT:
            display = new PresentDisplay();
            break;
        default:
            throw new IllegalArgumentException(
                "Unknown payload requested [type=" + message.payload.getType() + "]");
        }

        display.init(convoId, message);
        return display;
    }

    /**
     * Returns the {@link Widget} to be displayed to the recipient of this message.  This object
     * may (and typically will) contain active UI components to initiate requests to the
     * server. May be null, in which case nothing is displayed to the recipient.
     */
    public Widget widgetForRecipient ()
    {
        return null;
    }

    /**
     * Returns a {@link Widget} to display to the author of this message. This object is meant to
     * illustrate to an observer what the message looks like to the recipient, but any UI
     * components it includes should be inactive. May be null, in which case nothing is displayed
     * to the viewer.
     */
    public Widget widgetForSender ()
    {
        return null;
    }

    /**
     * Initializes this display with its bits.
     */
    protected void init (int convoId, ConvMessage message)
    {
        _convoId = convoId;
        _message = message;
        didInit();
    }

    /**
     * Allows derived classes to finish their initialization.
     */
    protected void didInit ()
    {
    }

    /**
     * Performs a server request to update the state for this message. If the callback
     * argument is null, one is created for you which does nothing on success and throws
     * a RuntimeException on failure.
     */
    protected void updateState (MailPayload payload, AsyncCallback<Void> callback)
    {
        if (callback == null) {
            callback = new AsyncCallback<Void>() {
                public void onSuccess (Void result) {
                }
                public void onFailure (Throwable caught) {
                    throw new RuntimeException(caught);
                }
            };
        }
        CMail.mailsvc.updatePayload(
            CMail.ident, _convoId, _message.sent.getTime(), payload, callback);
    }

    protected int _convoId;
    protected ConvMessage _message;
}
