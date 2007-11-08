//
// $Id$

package client.msgs;

import client.msgs.MailComposition.MailSentListener;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.data.MailPayload;

/**
 * Assist the {@link MailComposition} UI in configuring the state of a {@link MailPayload}.
 *
 * Some {@link MailPayload} subclasses are best configured visually as part of the {@link
 * MailComposition} UI. In such cases, an object should implement this interface and use {@link
 * #widgetForComposition()} to hand out a Widget with the appropriate configuration elements.
 *
 * When the message is ready for delivery, {@link #getComposedPayload()} is called, and the {@link
 * MailPayload} returned (if any) is included in the message.
 */
public interface MailPayloadComposer extends MailSentListener
{
    /**
     * Returns the {@link Widget} to be displayed to the composer of this message.  The widget will
     * tyically contain active UI components with which the sender configures the payload. May be
     * null, in which case nothing is displayed to the composer.
     */
    public Widget widgetForComposition ();

    /**
     * Asks if this composision widget ss ready to be sent, in which case it should return null;
     * if not, a string giving the reason is returned and displayed to the user. This is mostly
     * used for multi-stage UI's.
     *
     * TODO: Ideally this should be some kind of send-button-outgraying callback mechanism, but
     * that may be over the top.
     */
    public String okToSend ();

    /**
     * Returns the {@link MailPayload} to be included in the message being composed.  Typically,
     * the value has been configured according to the user's desire through the widget supplied by
     * {@link #widgetForComposition()}. This method may return null, in which case no {@link
     * MailPayload} is sent.
     */
    public MailPayload getComposedPayload ();
}
