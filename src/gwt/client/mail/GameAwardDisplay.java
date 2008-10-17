//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.mail.gwt.GameAwardPayload;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.ThumbBox;
import client.util.Link;
import client.util.NaviUtil;

/**
 * Displays game award mail payloads.
 */
public class GameAwardDisplay extends MailPayloadDisplay
{
    @Override // from MailPayloadDisplay
    public Widget widgetForSender ()
    {
        FlexTable table = new FlexTable();

        table.setWidget(0, 0, new ThumbBox(_payload.getAwardMedia(), null));
        table.getFlexCellFormatter().setRowSpan(0, 0, 2);

        table.setText(0, 1, _msgs.awardTitle());
        String args = NaviUtil.gameDetail(_payload.gameId, NaviUtil.GameDetails.TROPHIES);
        table.setWidget(0, 2, Link.create(_payload.gameName, Pages.GAMES, args));

        switch (_payload.awardType) {
        case GameAwardPayload.TROPHY:
            table.setText(1, 0, _msgs.trophyName());
            break;
        case GameAwardPayload.PRIZE:
            table.setText(1, 0, _msgs.prizeName());
            break;
        }
        table.setText(1, 1, _payload.awardName);

        return table;
    }

    @Override // from MailPayloadDisplay
    protected void didInit ()
    {
        _payload = (GameAwardPayload)_message.payload;
    }

    protected GameAwardPayload _payload;

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
}
