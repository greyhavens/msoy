//
// $Id$

package client.groups;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.GroupService.MedalsResult;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class MedalListPanel extends FlowPanel
{
    public MedalListPanel (int groupId)
    {
        setStyleName("medalListPanel");
        _groupId = groupId;
        _groupsvc.getAwardedMedals(groupId, new MsoyCallback<MedalsResult>() {
            public void onSuccess (MedalsResult result) {
                CShell.frame.setTitle(result.groupName.toString());
                displayMedals(result.medals);
            }
        });
    }

    protected void displayMedals (Map<Medal, List<VizMemberName>> medals)
    {
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        header.add(new Image("/images/group/medal_title.png"), 30, 0);
        header.add(MsoyUI.createLabel(_msgs.medalListIntro(), "Intro"), 235, 10);
        header.add(new Image("/images/group/medal_tofu.png"), 466, 0);
        add(header);

        RoundBox medalList = new RoundBox(RoundBox.MEDIUM_BLUE);
        add(medalList);
        medalList.addStyleName("MedalList");
        HorizontalPanel title = new HorizontalPanel();
        title.setStyleName("MedalTitle");
        title.add(new Label(_msgs.medalListHeader()));
        if (CShell.isSupport()) {
            title.add(MsoyUI.createActionLabel(
                _msgs.medalListAddMedal(), "AddMedal", Link.createListener(
                    Pages.GROUPS, GroupsPage.Nav.CREATEMEDAL.composeArgs(_groupId))));
        }
        medalList.add(title);
    }

    protected int _groupId;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
