//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a member's groups.
 */
public class MyGroupsPanel extends FlowPanel
{
    public MyGroupsPanel ()
    {
        setStyleName("myGroups");
        add(MsoyUI.createNowLoading());

        _groupsvc.getMyGroups(new InfoCallback<List<GroupCard>>() {
            public void onSuccess (List<GroupCard> groups) {
                init(groups);
            }
        });
    }

    protected void init (List<GroupCard> groups)
    {
        SmartTable grid = new SmartTable("Grid", 0, 10);
        int row = 0, col = 0;
        for (GroupCard card : groups) {
            grid.setWidget(row, col, new GroupBox(card));
            if (++col == GROUP_COLS) {
                ++row;
                col = 0;
            }
        }
        clear();
        add(grid);
    }

    protected static class GroupBox extends SmartTable
    {
        public GroupBox (GroupCard card)
        {
            super("Card", 0, 5);

            int groupId = card.name.getGroupId();
            setWidget(0, 0, new ThumbBox(card.getLogo(), MediaDesc.HALF_THUMBNAIL_SIZE,
                                         Pages.GROUPS, "d", groupId), 1, "Thumb");

            setWidget(0, 1, Link.create(""+card.name, Pages.GROUPS, "d", groupId));
            getWidget(0, 1).addStyleName("Bold");
            setText(0, 2, _msgs.mgMembers(""+card.memberCount), 1, "Link");

            setWidget(1, 0, MsoyUI.createLabel(card.blurb, "Blurb"), 1, null);
            getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

            String hmsg = (card.population == 0) ?
                _msgs.mgEnterHall() : _msgs.mgInHall(""+card.population);
            Widget hall = Link.create(hmsg, Pages.WORLD, "s" + card.homeSceneId);
            if (card.population > 0) {
                hall.addStyleName("Bold");
            }
            setWidget(1, 1, hall, 1, "Link");

            setWidget(2, 0, Link.create(_msgs.mgMedals(), Pages.GROUPS, "m", groupId), 1, "Thumb");
            setWidget(2, 1, Link.create(_msgs.mgDiscussions(), Pages.GROUPS, "f", groupId), 1,
                      "Link");

            getFlexCellFormatter().setRowSpan(0, 0, getRowCount()-1);
            getFlexCellFormatter().setRowSpan(1, 0, getRowCount()-1);
        }
    }

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);

    protected static final int GROUP_COLS = 2;
}
