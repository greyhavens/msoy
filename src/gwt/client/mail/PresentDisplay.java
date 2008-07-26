//
// $Id$

package client.mail;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.data.PresentPayload;

import client.shell.Args;
import client.shell.Page;
import client.util.Link;
import client.util.MsoyUI;
import client.util.ThumbBox;

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
                    Page.STUFF, Args.compose("d", ""+payload.ident.type, ""+payload.ident.itemId));
                setWidget(0, 1, WidgetUtil.makeShim(10, 10));
                setText(0, 2, CMail.msgs.presentRecipTip());
            }

            setWidget(0, 0, new ThumbBox(payload.getThumbnailMedia(), onClick));
            setWidget(1, 0, MsoyUI.createActionLabel(payload.name, onClick));
            getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
        }
    }
}
