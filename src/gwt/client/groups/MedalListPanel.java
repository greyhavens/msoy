//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;

import client.ui.MsoyUI;
import client.util.ServiceUtil;

public class MedalListPanel extends FlowPanel
{
    public MedalListPanel (GroupDetail detail, DetailContentPanel contentPanel)
    {
        _detail = detail;
        _contentPanel = contentPanel;
        // TODO: ask the server for the current medals and display them.
        displayMedals();
    }

    public void restoreListPanel (boolean refresh)
    {
        _contentPanel.showMedals();
    }

    protected void displayMedals ()
    {
        final String createMedal = _msgs.medalListCreateMedal();
        add(MsoyUI.createActionLabel(createMedal, new ClickListener () {
            public void onClick (Widget sender) {
                Medal medal = new Medal(_detail.group.groupId);
                _contentPanel.showSubContent(
                    createMedal, new EditMedalPanel(medal, MedalListPanel.this), true);
            }
        }));
    }

    protected GroupDetail _detail;
    protected DetailContentPanel _contentPanel;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
