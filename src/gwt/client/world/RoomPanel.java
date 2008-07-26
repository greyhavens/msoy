//
// $Id$

package client.world;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.gwt.Comment;
import com.threerings.msoy.web.data.RoomInfo;

import client.shell.CommentsPanel;
import client.shell.Frame;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.StyledTabPanel;

/**
 * Displays information about a room, allows commenting.
 */
public class RoomPanel extends SmartTable
{
    public RoomPanel (int sceneId)
    {
        super("roomPanel", 0, 5);

        CWorld.worldsvc.loadRoomInfo(sceneId, new MsoyCallback<RoomInfo>() {
            public void onSuccess (RoomInfo info) {
                init(info);
            }
        });
    }

    protected void init (RoomInfo info)
    {
        if (info == null) {
            setText(0, 0, "That room does not exist.");
            return;
        }
        Frame.setTitle(info.name);

        FlowPanel obits = new FlowPanel();
        obits.add(new InlineLabel(CWorld.msgs.owner(), false, false, true));
        if (info.owner instanceof MemberName) {
            MemberName name = (MemberName)info.owner;
            obits.add(Link.memberView(name.toString(), name.getMemberId()));
        } else if (info.owner instanceof GroupName) {
            GroupName name = (GroupName)info.owner;
            obits.add(Link.groupView(name.toString(), name.getGroupId()));
        }
        addWidget(obits, 1, null);

        StyledTabPanel tabs = new StyledTabPanel();
        tabs.add(new CommentsPanel(Comment.TYPE_ROOM, info.sceneId), CWorld.msgs.tabComments());
        addWidget(tabs, 1, null);
        tabs.selectTab(0);
    }
}
