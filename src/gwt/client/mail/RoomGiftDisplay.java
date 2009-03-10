//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.mail.gwt.RoomGiftPayload;

import client.room.RoomWidget;

/**
 * Displays item gift mail payloads.
 */
public class RoomGiftDisplay extends MailPayloadDisplay
{
    @Override
    public Widget widgetForRecipient ()
    {
        return new DisplayWidget((RoomGiftPayload) _message.payload, true);
    }

    @Override
    public Widget widgetForSender ()
    {
        return new DisplayWidget((RoomGiftPayload) _message.payload, false);
    }

    protected static class DisplayWidget extends SmartTable
    {
        public DisplayWidget (RoomGiftPayload  payload, boolean recipient)
        {
            super(0, 0);

            setWidget(0, 0, new RoomWidget(payload.sceneId, payload.name, payload.thumbnail));
            setWidget(0, 1, WidgetUtil.makeShim(10, 10));
            setText(0, 2, recipient ? _msgs.roomGiftRecipTip() : _msgs.roomGiftSenderTip());
        }
    }

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
}
