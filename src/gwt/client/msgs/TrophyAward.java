//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.TrophyAwardPayload;

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
            _payload = (TrophyAwardPayload) message.payload;
        }

        // @Override // from MailPayloadDisplay
        public Widget widgetForRecipient (MailUpdateListener listener)
        {
            FlexTable table = new FlexTable();
            table.setStyleName("trophyAward");
            table.setText(0, 0, CMsgs.mmsgs.trophyTitle(_payload.gameName));
            table.setText(1, 0, _payload.trophyName);
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
