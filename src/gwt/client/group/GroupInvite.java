//
// $Id$

package client.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.mail.MailBodyObjectComposer;
import client.mail.MailBodyObjectDisplay;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MailBodyObject;

public abstract class GroupInvite
{
    public static void getInvitationGroups (WebContext _ctx, AsyncCallback callback)
    {
        _ctx.groupsvc.getMembershipGroups(_ctx.creds, _ctx.creds.memberId, true, callback);
    }

    public static final class Composer
        implements MailBodyObjectComposer
    {
        // @Override
        public MailBodyObject getComposedObject ()
        {
            Map state = new HashMap();
            state.put(STATE_GROUP_ID_KEY, new Integer(_selectedGroupId));
            return new MailBodyObject(MailBodyObject.TYPE_GROUP_INVITE, state);
        }

        // @Override
        public Widget widgetForComposition (WebContext ctx)
        {
            return new CompositionWidget(ctx);
        }

        protected Composer (List groups)
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
        
        protected List _groups;

        protected int _selectedGroupId = -1;
    }
    
    public static final class Display extends MailBodyObjectDisplay
    {
        public Display (Map state)
        {
            if (state != null) {
                Object idObj = state.get(STATE_GROUP_ID_KEY);
                if (idObj != null && idObj instanceof Integer) {
                    _invitationGroupId = ((Integer) idObj).intValue();
                }
            }
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

        protected Display (int groupId)
        {
            _invitationGroupId = groupId;
        }

        protected int _invitationGroupId = -1;
    }

    public static final String STATE_GROUP_ID_KEY = "groupId";
    
}
