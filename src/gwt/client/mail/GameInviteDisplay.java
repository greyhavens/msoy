//
// $Id$

package client.mail;

import client.util.Link;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.mail.gwt.GameInvitePayload;

import com.threerings.msoy.web.gwt.Pages;

/**
 * Displays a game invitation.
 */
public class GameInviteDisplay extends MailPayloadDisplay
{
    @Override // from MailPayloadDisplay
    public Widget widgetForRecipient ()
    {
        SmartTable widget = new SmartTable(0, 0);
        widget.setText(0, 0, _msgs.gameInvitation(), 0, "rowPanelCell");
        widget.setWidget(0, 1, new Button(_msgs.gameBtnPlay(), new ClickHandler () {
            public void onClick (ClickEvent event) {
                Link.go(Pages.WORLD, _payload.args);
            }
        }));
        return widget;
    }

    @Override // from MailPayloadDisplay
    protected void didInit ()
    {
        _payload = (GameInvitePayload)_message.payload;
    }

    protected GameInvitePayload _payload;
    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
}
