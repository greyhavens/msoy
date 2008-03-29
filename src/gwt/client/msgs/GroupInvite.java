//
// $Id$

package client.msgs;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.GroupMemberCard;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.person.data.GroupInvitePayload;
import com.threerings.msoy.person.data.MailMessage;
import com.threerings.msoy.person.data.MailPayload;
import com.threerings.msoy.web.client.GroupService;

import client.util.MsoyCallback;

public abstract class GroupInvite
{
    public static void getInvitationGroups (AsyncCallback callback)
    {
        CMsgs.groupsvc.getMembershipGroups(CMsgs.ident, CMsgs.getMemberId(), true, callback);
    }

    public static final class Composer
        implements MailPayloadComposer
    {
        public Composer (List groups)
        {
            _groups = groups;
        }

        // from MailPayloadComposer
        public MailPayload getComposedPayload ()
        {
            return new GroupInvitePayload(_selectedGroupId, false);
        }

        // from MailPayloadComposer
        public Widget widgetForComposition ()
        {
            return new CompositionWidget();
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
            // TODO: if we implement backend tracking of group invites, do something here.
        }

        protected class CompositionWidget extends HorizontalPanel
        {
            public CompositionWidget ()
            {
                super();
                
                // set up the recipient/subject header grid
                Grid grid = new Grid(1, 2);
                CellFormatter formatter = grid.getCellFormatter();
                grid.setStyleName("Headers");

                _groupBox = new ListBox();
                _groupBox.setSelectedIndex(0);
                for (int ii = 0; ii < _groups.size(); ii ++) {
                    GroupName group = ((GroupMembership) _groups.get(ii)).group;
                    _groupBox.addItem(group.toString());
                    if (ii == 0) {
                        // let the first group be the default
                        _selectedGroupId = group.getGroupId();
                    }
                }
                _groupBox.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        int ix = _groupBox.getSelectedIndex();
                        if (ix == -1) {
                            return;
                        }
                        _selectedGroupId = ((GroupMembership)_groups.get(ix)).group.getGroupId();
                    }
                });
                grid.setText(0, 0, CMsgs.mmsgs.groupInviteTo());
                formatter.setStyleName(0, 0, "Label");
                formatter.setWidth(0, 0, "6em");
                grid.setWidget(0, 1, _groupBox);
                formatter.setStyleName(0, 1, "Value");

                add(grid);
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
            _invitePayload = (GroupInvitePayload) message.payload;
        }

        // @Override
        public Widget widgetForRecipient (MailUpdateListener listener)
        {
            _listener = listener;
            return new DisplayWidget(_invitePayload.responded == false);
        }

        // @Override
        public Widget widgetForOthers ()
        {
            return new DisplayWidget(false);
        }

        // @Override
        public String okToDelete ()
        {
            // we're always happy to be deleted
            return null;
        }

        protected class DisplayWidget extends DockPanel
        {
            public DisplayWidget (boolean enabled)
            {
                super();
                _enabled = enabled;
                setStyleName("GroupInvitation");
                
                _status = new Label();
                add(_status, DockPanel.SOUTH);
                _content = new FlowPanel();
                add(_content, DockPanel.CENTER);

                refreshUI();
            }
             
            protected void refreshUI ()
            {
                CMsgs.groupsvc.getGroupMembers(
                    CMsgs.ident, _invitePayload.groupId, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        _name = ((GroupService.MembersResult) result).name.toString();
                        _members = ((GroupService.MembersResult) result).members;
                        buildUI();
                    }
                    public void onFailure (Throwable caught) {
                        _status.setText(CMsgs.serverError(caught));
                    }
                });
            }

            protected void buildUI ()
            {
                _content.clear();
                for (Iterator members = _members.iterator(); members.hasNext(); ) {
                    GroupMemberCard card = (GroupMemberCard) members.next();
                    if (CMsgs.creds.name.equals(card.name)) {
                        _content.add(new InlineLabel(CMsgs.mmsgs.groupAlreadyMember(_name)));
                        return;
                    }
                }
                _content.add(new InlineLabel(CMsgs.mmsgs.groupInvitation(_name), true, false, true));
                Button joinButton = new Button(CMsgs.mmsgs.groupBtnJoin());
                joinButton.addStyleName("JoinButton");
                joinButton.setEnabled(_enabled);
                joinButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        joinGroup();
                        refreshUI();
                    }
                });
                _content.add(joinButton);
            }

            protected void joinGroup ()
            {
                CMsgs.groupsvc.joinGroup(CMsgs.ident, _invitePayload.groupId, new MsoyCallback() {
                    // if joining the group succeeds, mark this invitation as accepted
                    public void onSuccess (Object result) {
                        _invitePayload.responded = true;
                        updateState(_invitePayload, new MsoyCallback() {
                            // and if that succeded to, let the mail app know to refresh
                            public void onSuccess (Object result) {
                                if (_listener != null) {
                                    _listener.messageChanged(_message.headers.folderId,
                                                             _message.headers.messageId);
                                }
                            }
                        });
                    }
                });
            }

            protected boolean _enabled;
            
            protected String _name;
            protected List _members;
            
            protected Label _status;
            protected FlowPanel _content;
        }

        protected GroupInvitePayload _invitePayload;
        protected MailUpdateListener _listener;
    }
}
