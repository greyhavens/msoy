package client.mail;

import java.util.Map;

import client.group.GroupInvite;
import client.person.FriendInvite;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailBodyObject;
import com.threerings.msoy.web.data.MailMessage;

/**
 * Base class for body object visualizers. Concrete subclasses of this object are configured
 * with a {@link WebContext} and a {@link MailMessage}, and will be asked to hand out Widgets
 * to be displayed in mail messages in the GTW Mail system through the functions
 * {@link #widgetForRecipient()} and {@link #widgetForOthers()).
 */
public abstract class MailBodyObjectDisplay
{
    /**
     * Constructs and retursn the appropriate {@link MailBodyObjectDisplay} for the 
     * given mail message (presuming it has a body object).
     */
    public static MailBodyObjectDisplay getDisplay (WebContext ctx, MailMessage message)
    {
        if (message.bodyObject == null) {
            return null;
        }
        switch(message.bodyObject.getType()) {
        case MailBodyObject.TYPE_GROUP_INVITE:
            return new GroupInvite.Display(ctx, message);
        case MailBodyObject.TYPE_FRIEND_INVITE:
            return new FriendInvite.Display(ctx, message);
        }
        throw new IllegalArgumentException(
            "Unknown body object requested [type=" + message.bodyObject.getType() + "]");
    }

    public MailBodyObjectDisplay (WebContext ctx, MailMessage message)
    {
        _ctx = ctx;
        _message = message;
    }
    
    /**
     *  Returns the {@link Widget} to be displayed to the recipient of this message.
     *  This object may (and typically will) contain active UI components to initiate
     *  requests to the server. May be null, in which case nothing is displayed to
     *  the recipient.
     */
    public abstract Widget widgetForRecipient (MailUpdateListener listener);

    /**
     *  Returns a {@link Widget} to display to anybody who is not this message's recipient.
     *  This object is meant to illustrate to an observer what the message looks like to
     *  the recipient, but any UI components it includes should be inactive. May be null,
     *  in which case nothing is displayed to the viewer.
     */
    public abstract Widget widgetForOthers ();

    /**
     * Performs a server request to update the state for this message. If the callback
     * argument is null, one is created for you which does nothing on success and throws
     * a RuntimeException on failure.
     */
    protected void updateState (MailBodyObject newObj, AsyncCallback callback)
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
        _ctx.mailsvc.updateBodyObject(_ctx.creds, _message.headers.folderId,
                                      _message.headers.messageId, newObj, callback);
    }

    protected WebContext _ctx;
    protected MailMessage _message;
}
