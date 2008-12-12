//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.ServiceUtil;

public class MedalListPanel extends FlowPanel
{
    public MedalListPanel (int groupId)
    {
        _groupId = groupId;
        // TODO: ask the server for the current medals and display them.
        displayMedals();
    }

    protected void displayMedals ()
    {
        add(MsoyUI.createActionLabel(_msgs.medalListAddMedal(),
            Link.createListener(Pages.GROUPS, GroupsPage.Nav.CREATEMEDAL.composeArgs(_groupId))));
    }

    protected int _groupId;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
