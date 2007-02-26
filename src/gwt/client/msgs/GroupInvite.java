//
// $Id$

package client.msgs;

import java.util.List;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.data.GroupInviteObject;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MemberName;

public abstract class GroupInvite
{
    public static void getInvitationGroups (AsyncCallback callback)
    {
        CMsgs.groupsvc.getMembershipGroups(CMsgs.creds, CMsgs.getMemberId(), true, callback);
    }

    public static final class Composer
        implements MailPayloadComposer
    {
        public Composer (List groups)
        {
            _groups = groups;
        }

        // @Override
        public MailPayload getComposedPayload ()
        {
            return new GroupInviteObject(_selectedGroupId, false);
        }

        // @Override
        public Widget widgetForComposition ()
        {
            return new CompositionWidget();
        }

        // @Override
        public void messageSent (MemberName recipient)
        {
            // TODO: if we implement backend tracking of group invites, do something here.
        }

        protected class CompositionWidget extends HorizontalPanel
        {
            public CompositionWidget ()
            {
                super();
                FlowPanel panel = new FlowPanel();
                panel.add(new InlineLabel(CMsgs.mmsgs.groupClick(), false, false, true));
                Button joinButton = new Button(CMsgs.mmsgs.groupJoin());
                joinButton.setEnabled(false);
                panel.add(joinButton);
                panel.add(new InlineLabel(CMsgs.mmsgs.groupThe(), false, true, true));
                _groupBox = new ListBox();
                for (int ii = 0; ii < _groups.size(); ii ++) {
                    _groupBox.addItem(((GroupMembership) _groups.get(ii)).group.groupName);
                }
                _groupBox.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        int ix = _groupBox.getSelectedIndex();
                        if (ix == -1) {
                            return;
                        }
                        _selectedGroupId = ((GroupMembership)_groups.get(ix)).group.groupId;
                        Window.alert("groupId now = " + _selectedGroupId);
                    }
                });
                panel.add(_groupBox);
                add(panel);
            }
            protected ListBox _groupBox;
        }

        protected List _groups;
        protected int _selectedGroupId = -1;
    }

    public static final class Display extends MailPayloadDisplay
    {
        public Display (MailMessage message)
        {
            super(message);
            // no sanity checks: if anything breaks here, it's already a disaster
            _inviteObject = (GroupInviteObject) message.payload;
        }

        // @Override
        public Widget widgetForRecipient (MailUpdateListener listener)
        {
            _listener = listener;
            return new DisplayWidget(_inviteObject.responded == false);
        }

        // @Override
        public Widget widgetForOthers ()
        {
            return new DisplayWidget(false);
        }

        protected class DisplayWidget extends HorizontalPanel
        {
            public DisplayWidget (boolean enabled)
            {
                super();
                FlowPanel panel = new FlowPanel();
                panel.add(new InlineLabel(CMsgs.mmsgs.groupClick(), false, false, true));
                Button joinButton = new Button(CMsgs.mmsgs.groupJoin());
                joinButton.setEnabled(enabled);
                joinButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        joinGroup();
                    }
                });
                panel.add(joinButton);
                panel.add(new InlineLabel(CMsgs.mmsgs.groupThe(), false, true, false));
                add(panel);
            }

            protected void joinGroup ()
            {
                CMsgs.groupsvc.joinGroup(CMsgs.creds, _inviteObject.groupId,
                                         CMsgs.getMemberId(), new AsyncCallback() {
                    // if joining the group succeeds, mark this invitation as accepted
                    public void onSuccess (Object result) {
                        _inviteObject.responded = true;
                        updateState(_inviteObject, new AsyncCallback() {
                            // and if that succeded to, let the mail app know to refresh
                            public void onSuccess (Object result) {
                                if (_listener != null) {
                                    _listener.messageChanged(_message.headers.ownerId,
                                                             _message.headers.folderId,
                                                             _message.headers.messageId);
                                }
                            }
                            public void onFailure (Throwable caught) {
                                // TODO: General support for errors in the MailApplication API?
                            }
                        });
                    }
                    public void onFailure (Throwable caught) {
                        // TODO: General support for errors in the MailApplication API?
                    }
                });
            }

            protected ListBox _groupBox;
        }

        protected GroupInviteObject _inviteObject;
        protected MailUpdateListener _listener;
    }
}
