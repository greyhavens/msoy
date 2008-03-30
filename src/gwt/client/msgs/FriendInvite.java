//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.data.FriendInvitePayload;
import com.threerings.msoy.person.data.MailPayload;

import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.MsoyCallback;

public abstract class FriendInvite
{
    public static class Composer
        implements MailPayloadComposer
    {
        // from MailPayloadComposer
        public Widget widgetForComposition ()
        {
            return new InvitationWidget();
        }

        // from MailPayloadComposer
        public MailPayload getComposedPayload ()
        {
            return new FriendInvitePayload();
        }

        // from MailPayloadComposer
        public String okToSend ()
        {
            // we're always ready to be sent
            return null;
        }

        // from MailPayloadComposer
        public void messageSent (MemberName recipient)
        {
            // the successful dispatch of an invite finishes one of the tutorial steps
            FlashClients.tutorialEvent("friendInvited");
        }

        /**
         * A miniature version of the widget displayed by the mail reader.
         */
        protected class InvitationWidget extends FlowPanel
        {
            protected InvitationWidget ()
            {
                super();
                add(new InlineLabel(CMsgs.mmsgs.friendInviting()));
            }
        }
    }
}
