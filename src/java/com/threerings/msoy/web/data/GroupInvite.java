//
// $Id$

package com.threerings.msoy.web.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;

public class GroupInvite
    implements MailBodyObjectComposer
{
    // @Override
    public MailBodyObject getComposedWidget ()
    {
        return new GroupBodyObject(_selectedGroupId);
    }

    // @Override
    public Widget widgetForComposition (WebContext ctx)
    {
        return new CompositionWidget(ctx);
    }

    public static final class GroupBodyObject extends MailBodyObject
    {
        public static final String STATE_GROUP_ID_KEY = "groupId";

        public static void getInvitationGroups (WebContext _ctx, AsyncCallback callback)
        {
            _ctx.groupsvc.getMembershipGroups(_ctx.creds, _ctx.creds.memberId, true, callback);
        }

        public GroupBodyObject ()
        {
            this(-1);
        }
        
        public GroupBodyObject (Map state)
        {
            if (state != null) {
                Object idObj = state.get(STATE_GROUP_ID_KEY);
                if (idObj != null && idObj instanceof Integer) {
                    _invitationGroupId = ((Integer) idObj).intValue();
                }
            }
        }
        
        // @Override
        public int getType ()
        {
            return TYPE_GROUP_INVITE;
        }

        // @Override
        public Map exportState ()
        {
            Map state = new HashMap();
            state.put(STATE_GROUP_ID_KEY, new Integer(_invitationGroupId));
            return state;
        }

        // @Override
        public Widget widgetForRecipient (WebContext ctx)
        {
            Button button = new Button("GROUP INVITE");
            button.setEnabled(true);
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Window.alert("I have been clickz0red!");
                }
            });
            return button;
        }


        // @Override
        public Widget widgetForOthers (WebContext ctx)
        {
            return null;
        }

        // @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof GroupBodyObject)) {
                return false;
            }
            return _invitationGroupId == ((GroupBodyObject) obj)._invitationGroupId;
        }

        // @Override
        public int hashCode ()
        {
            return _invitationGroupId;
        }
        
        protected GroupBodyObject (int groupId)
        {
            _invitationGroupId = groupId;
        }

        protected int _invitationGroupId = -1;
    }
    
    protected GroupInvite (List groups)
    {
        _groups = groups;
    }

    protected class CompositionWidget extends DockPanel
    {
        public CompositionWidget (WebContext ctx)
        {
            super();
            _ctx = ctx;
            setWidth("100%");
            _groupBox = new ListBox();
            _groupBox.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    int ix = _groupBox.getSelectedIndex();
                    if (ix == -1) {
                        return;
                    }
                    _selectedGroupId = ((GroupMembership)_groups.get(ix)).groupId;
                }
            });
        }
        protected WebContext _ctx;
        protected List _groupMemberships;
        protected ListBox _groupBox;
    }
    
    protected transient List _groups;

    protected int _selectedGroupId = -1;
}
