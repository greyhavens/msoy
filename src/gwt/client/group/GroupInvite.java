//
// $Id$

package client.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.mail.MailPayloadComposer;
import client.mail.MailPayloadDisplay;
import client.mail.MailUpdateListener;
import client.util.InlineLabel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.GroupInviteObject;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MemberGName;

public abstract class GroupInvite
{

    public static void getInvitationGroups (WebContext _ctx, AsyncCallback callback)
    {
        _ctx.groupsvc.getMembershipGroups(_ctx.creds, _ctx.creds.memberId, true, callback);
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
        public Widget widgetForComposition (WebContext ctx)
        {
            return new CompositionWidget(ctx);
        }

        // @Override
        public void messageSent (WebContext ctx, MemberGName recipient)
        {
            // TODO: if we implement backend tracking of group invites, do something here.
        }

        protected class CompositionWidget extends HorizontalPanel
        {
            public CompositionWidget (WebContext ctx)
            {
                super();
                _ctx = ctx;
                setWidth("100%");
                add(new InlineLabel("Click to "));
                Button joinButton = new Button("join");
                joinButton.setEnabled(false);
                add(joinButton);
                add(new InlineLabel(" the group "));
                _groupBox = new ListBox();
                for (int ii = 0; ii < _groups.size(); ii ++) {
                    _groupBox.addItem(((GroupMembership) _groups.get(ii)).groupName);
                }
                _groupBox.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        int ix = _groupBox.getSelectedIndex();
                        if (ix == -1) {
                            return;
                        }
                        _selectedGroupId = ((GroupMembership)_groups.get(ix)).groupId;
                        Window.alert("groupId now = " + _selectedGroupId);
                    }
                });
                add(_groupBox);
            }
            protected WebContext _ctx;
            protected ListBox _groupBox;
        }
        
        protected List _groups;
        protected int _selectedGroupId = -1;
    }
    
    public static final class Display extends MailPayloadDisplay
    {
        public Display (WebContext ctx, MailMessage message)
        {
            super(ctx, message);
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
                add(new InlineLabel("Click to "));
                Button joinButton = new Button("join");
                joinButton.setEnabled(enabled);
                joinButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        joinGroup();
                    }
                });
                add(joinButton);
                add(new InlineLabel(" the group #" + _inviteObject.groupId + "."));
            }
            
            protected void joinGroup ()
            {
                _ctx.groupsvc.leaveGroup(
                    _ctx.creds, _inviteObject.groupId, _ctx.creds.memberId, new AsyncCallback() {
                        // if leaving the group succeeds, mark this invitation as accepted
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
