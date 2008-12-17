//
// $Id$

package client.rooms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.room.gwt.RoomDetail;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.comment.CommentsPanel;
import client.room.SceneUtil;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.Rating;
import client.ui.StyledTabPanel;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays information about a room, allows commenting.
 */
public class RoomDetailPanel extends SmartTable
{
    public RoomDetailPanel (int sceneId)
    {
        super("roomDetailPanel", 0, 10);

        _worldsvc.loadRoomDetail(sceneId, new MsoyCallback<RoomDetail>() {
            public void onSuccess (RoomDetail detail) {
                init(detail);
            }
        });
    }

    protected void init (final RoomDetail detail)
    {
        if (detail == null) {
            setText(0, 0, "That room does not exist.");
            return;
        }
        CShell.frame.setTitle(detail.info.name);

        setWidget(0, 0, SceneUtil.createSceneView(detail.info.sceneId, detail.snapshot));
        final int snapWidth = MediaDesc.getWidth(MediaDesc.SNAPSHOT_FULL_SIZE);
        getFlexCellFormatter().setWidth(0, 0, snapWidth + "px");

        FlowPanel obits = new FlowPanel();
        obits.add(MsoyUI.createLabel(detail.info.name, "Title"));
        obits.add(new InlineLabel(_msgs.owner(), false, false, true));
        if (detail.owner instanceof MemberName) {
            MemberName name = (MemberName)detail.owner;
            obits.add(Link.memberView(name.toString(), name.getMemberId()));
        } else if (detail.owner instanceof GroupName) {
            GroupName name = (GroupName)detail.owner;
            obits.add(Link.groupView(name.toString(), name.getGroupId()));
        }
        obits.add(WidgetUtil.makeShim(10, 15));
        obits.add(new Rating(detail.info.rating, detail.ratingCount, detail.memberRating, true) {
            @Override
            protected void handleRate (byte newRating , MsoyCallback<RatingResult> callback) {
                _worldsvc.rateRoom(detail.info.sceneId, newRating, callback);
            }
        });
        setWidget(0, 1, obits);

        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);

        StyledTabPanel tabs = new StyledTabPanel();
        tabs.add(new CommentsPanel(Comment.TYPE_ROOM, detail.info.sceneId, true),
                 _msgs.tabComments());
        addWidget(tabs, 2, null);
        tabs.selectTab(0);
    }

    protected static final RoomsMessages _msgs = GWT.create(RoomsMessages.class);
    protected static final WebRoomServiceAsync _worldsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
