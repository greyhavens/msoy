//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;

import client.ui.MsoyUI;
import client.util.ServiceUtil;

public class MedalListPanel extends FlowPanel
{
    public MedalListPanel (GroupDetail detail, GroupDetailPanel.SubContentDisplay contentDisplay)
    {
        _detail = detail;
        _contentDisplay = contentDisplay;
        // TODO: ask the server for the current medals and display them.
        displayMedals();
    }

    protected void displayMedals ()
    {
        final String createMedal = _msgs.medalListCreateMedal();
        add(MsoyUI.createActionLabel(createMedal, new ClickListener () {
            public void onClick (Widget sender) {
                _contentDisplay.showContent(createMedal, new EditMedalPanel(null, _contentDisplay));
            }
        }));
    }

    protected GroupDetail _detail;
    protected GroupDetailPanel.SubContentDisplay _contentDisplay;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
