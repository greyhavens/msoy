//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.mail.gwt.PresentPayload;
import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;

import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays item gift mail payloads.
 */
public class PresentDisplay extends MailPayloadDisplay
{
    @Override
    public Widget widgetForRecipient ()
    {
        return new DisplayWidget((PresentPayload)_message.payload, true);
    }

    @Override
    public Widget widgetForSender ()
    {
        return new DisplayWidget((PresentPayload)_message.payload, false);
    }

    protected static class DisplayWidget extends SmartTable
    {
        public DisplayWidget (PresentPayload payload, boolean recipient)
        {
            super(0, 0);

            ClickListener onClick = null;
            if (recipient) {
                onClick = Link.createListener(
                    Pages.STUFF, Args.compose("d", ""+payload.ident.type, ""+payload.ident.itemId));
                setWidget(0, 1, WidgetUtil.makeShim(10, 10));
                setText(0, 2, _msgs.presentRecipTip());
            }

            setWidget(0, 0, new ThumbBox(payload.thumbMedia, onClick));
            setWidget(1, 0, MsoyUI.createActionLabel(payload.name, onClick));
            getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
        }
    }

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
}
