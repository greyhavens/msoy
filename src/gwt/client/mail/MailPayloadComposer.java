
//$Id$

package client.mail;

import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MemberName;

import client.util.WebContext;

/**
 * Assist the {@link MailComposition} UI in configuring the state of a {@link MailPayload}.
 * 
 * Some {@link MailPayload} subclasses are best configured visually as part of the
 * {@link MailComposition} UI. In such cases, an object should implement this interface
 * and use {@link #widgetForComposition(WebContext)} to hand out a Widget with the
 * appropriate configuration elements.
 * 
 * When the message is ready for delivery, {@link #getComposedPayload()} is called, and the
 * {@link MailPayload} returned (if any) is included in the message.
 */
public interface MailPayloadComposer
{
    /**
     * Returns the {@link Widget} to be displayed to the composer of this message.
     * The widget will tyically contain active UI components with which the sender
     * configures the payload. May be null, in which case nothing is displayed
     * to the composer.
     */
    public Widget widgetForComposition (WebContext ctx);

    /**
     * Returns the {@link MailPayload} to be included in the message being composed.
     * Typically, the value has been configured according to the user's desire through
     * the widget supplied by {@link #widgetForComposition(WebContext)}. This method may
     * return null, in which case no {@link MailPayload} is sent.
     */
    public MailPayload getComposedPayload ();
    
    /**
     * Notifies this object that the message being composed was successfully sent,
     * and that it may perform any side-effects that should be associated with the
     * event.
     */
    public void messageSent (WebContext _ctx, MemberName _recipient);
}
