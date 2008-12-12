//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.GroupService.MedalsResult;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class MedalListPanel extends FlowPanel
{
    public MedalListPanel (int groupId)
    {
        _groupsvc.getMedals(_groupId = groupId, new MsoyCallback<GroupService.MedalsResult>() {
            public void onSuccess (MedalsResult result) {
                CShell.frame.setTitle(result.groupName.toString());
                displayMedals(result.medals);
            }
        });
    }

    protected void displayMedals (List<Medal> medals)
    {
        add(MsoyUI.createActionLabel(_msgs.medalListAddMedal(),
            Link.createListener(Pages.GROUPS, GroupsPage.Nav.CREATEMEDAL.composeArgs(_groupId))));
    }

    protected int _groupId;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
