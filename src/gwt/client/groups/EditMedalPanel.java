package client.groups;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;

import client.util.ServiceUtil;

public class EditMedalPanel extends FlowPanel
{
    public EditMedalPanel ()
    {
        // TODO: Accept a current medal definition for editing.  For now this is really a
        // CreateMedalPanel
    }

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
