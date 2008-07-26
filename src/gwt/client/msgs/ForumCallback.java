//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.SourcesClickEvents;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.web.data.MessageTooLongException;

import client.util.ClickCallback;

/**
 * Extends {@link ClickCallback} and handles the special {@link MessageTooLongException}.
 */
public abstract class ForumCallback<T> extends ClickCallback<T>
{
    public ForumCallback (SourcesClickEvents trigger)
    {
        super(trigger);
    }

    @Override // from ClickCallback
    protected String convertError (Throwable cause) {
        if (cause instanceof MessageTooLongException) {
            int extra = ((MessageTooLongException)cause).getMessageLength() -
                ForumMessage.MAX_MESSAGE_LENGTH;
            return CMsgs.mmsgs.errMessageTooLong(""+extra);
        } else {
            return super.convertError(cause);
        }
    }
}
