//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MailMessage;

import com.threerings.msoy.web.data.TrophyAwardPayload;

import client.game.GameDetailPanel;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;

/**
 * Contains a renderer for a trophy award.
 */
public abstract class TrophyAward
{
    public static final class Display extends MailPayloadDisplay
    {
        public Display (MailMessage message)
        {
            super(message);
            _payload = (TrophyAwardPayload)message.payload;
        }

        // @Override // from MailPayloadDisplay
        public Widget widgetForRecipient (MailUpdateListener listener)
        {
            FlexTable table = new FlexTable();
            table.setText(0, 0, CMsgs.mmsgs.trophyTitle());
            String args = Args.compose(new String[] {
                "d", "" + _payload.gameId, GameDetailPanel.TROPHIES_TAB });
            table.setWidget(0, 1, Application.createLink(_payload.gameName, Page.GAME, args));
            table.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_CENTER);
            table.setWidget(1, 1, MediaUtil.createMediaView(
                                _payload.getTrophyMedia(), MediaDesc.THUMBNAIL_SIZE));
            table.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_CENTER);
            table.setText(2, 0, CMsgs.mmsgs.trophyName());
            table.setText(2, 1, _payload.trophyName);
            table.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_CENTER);
            return table;
        }

        // @Override // from MailPayloadDisplay
        public Widget widgetForOthers ()
        {
            throw new IllegalStateException("Non-recipients should not see trophy awards.");
        }

        // @Override // from MailPayloadDisplay
        public String okToDelete ()
        {
            return null;
        }

        protected TrophyAwardPayload _payload;
    }
}
