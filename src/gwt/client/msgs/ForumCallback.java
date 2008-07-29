//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.SourcesClickEvents;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.MessageTooLongException;

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
            return _mmsgs.errMessageTooLong(""+extra);
        } else {
            return super.convertError(cause);
        }
    }

    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
}
