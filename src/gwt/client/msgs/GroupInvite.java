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
}
