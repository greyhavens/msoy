//
// $Id$

package client.mail;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.person.data.GameAwardPayload;

import client.games.GameDetailPanel;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ThumbBox;

/**
 * Displays game award mail payloads.
 */
public class GameAwardDisplay extends MailPayloadDisplay
{
    // @Override // from MailPayloadDisplay
    public Widget widgetForRecipient ()
    {
        FlexTable table = new FlexTable();

        table.setWidget(0, 0, new ThumbBox(_payload.getAwardMedia(), null));
        table.getFlexCellFormatter().setRowSpan(0, 0, 2);

        table.setText(0, 1, CMail.msgs.awardTitle());
        String args = Args.compose(new String[] {
                "d", "" + _payload.gameId, GameDetailPanel.TROPHIES_TAB });
        table.setWidget(0, 2, Application.createLink(_payload.gameName, Page.GAMES, args));

        switch (_payload.awardType) {
        case GameAwardPayload.TROPHY:
            table.setText(1, 0, CMail.msgs.trophyName());
            break;
        case GameAwardPayload.PRIZE:
            table.setText(1, 0, CMail.msgs.prizeName());
            break;
        }
        table.setText(1, 1, _payload.awardName);

        return table;
    }

    // @Override // from MailPayloadDisplay
    public Widget widgetForOthers ()
    {
        throw new IllegalStateException("Non-recipients should not see game awards.");
    }

    // @Override // from MailPayloadDisplay
    public String okToDelete ()
    {
        return null;
    }

    // @Override // from MailPayloadDisplay
    protected void didInit ()
    {
        _payload = (GameAwardPayload)_message.payload;
    }

    protected GameAwardPayload _payload;
}
