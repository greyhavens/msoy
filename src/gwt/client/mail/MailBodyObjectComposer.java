
//$Id$

package client.mail;

import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailBodyObject;

/**
 * Assist the {@link MailComposition} UI in configuring the state of a {@link MailBodyObject}.
 * 
 * Some {@link MailBodyObject} subclasses are best configured visually as part of the
 * {@link MailComposition} UI. In such cases, an object should implement this interface
 * and use {@link #widgetForComposition(WebContext)} to hand out a Widget with the
 * appropriate configuration elements.
 * 
 * When the message is ready for delivery, {@link #getComposedObject()} is called, and the
 * {@link MailBodyObject} returned (if any) is included in the message.
 */
public interface MailBodyObjectComposer
{
    /**
     * Returns the {@link Widget} to be displayed to the composer of this message.
     * The widget will tyically contain active UI components with which the sender
     * configures the body object. May be null, in which case nothing is displayed
     * to the composer.
     */
    public Widget widgetForComposition (WebContext ctx);

    /**
     * Returns the {@link MailBodyObject} to be included in the message being composed.
     * Typically, the value has been configured according to the user's desire through
     * the widget supplied by {@link #widgetForComposition(WebContext)}. This method may
     * return null, in which case no {@link MailBodyObject} is sent.
     */
    public MailBodyObject getComposedObject ();
}
