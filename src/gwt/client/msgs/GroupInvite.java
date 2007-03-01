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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupInviteObject;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.GroupName;
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
                
                // set up the recipient/subject header grid
                Grid grid = new Grid(1, 2);
                CellFormatter formatter = grid.getCellFormatter();
                grid.setStyleName("Headers");

                _groupBox = new ListBox();
                _groupBox.setSelectedIndex(0);
                for (int ii = 0; ii < _groups.size(); ii ++) {
                    GroupName group = ((GroupMembership) _groups.get(ii)).group;
                    _groupBox.addItem(group.groupName);
                    if (ii == 0) {
                        // let the first group be the default
                        _selectedGroupId = group.groupId;
                    }
                }
                _groupBox.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        int ix = _groupBox.getSelectedIndex();
                        if (ix == -1) {
                            return;
                        }
                        _selectedGroupId = ((GroupMembership)_groups.get(ix)).group.groupId;
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
                CMsgs.groupsvc.getGroupDetail(
                    CMsgs.creds, _inviteObject.groupId, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        _detail = (GroupDetail) result;
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
                Iterator members = _detail.members.iterator();
                while (members.hasNext()) {
                    GroupMembership ship = (GroupMembership) members.next();
                    if (CMsgs.creds.name.equals(ship.member)) {
                        _content.add(new InlineLabel(
                            CMsgs.mmsgs.groupAlreadyMember(_detail.group.name)));
                        return;
                    }
                }
                _content.add(new InlineLabel(
                    CMsgs.mmsgs.groupInvitation(_detail.group.name), true, false, true));
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

            protected boolean _enabled;
            
            protected GroupDetail _detail;
            
            protected Label _status;
            protected FlowPanel _content;
        }

        protected GroupInviteObject _inviteObject;
        protected MailUpdateListener _listener;
    }
}
