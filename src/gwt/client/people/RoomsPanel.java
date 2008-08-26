//
// $Id: MyRoomsPanel.java 10984 2008-08-19 19:03:33Z mdb $

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;
import com.threerings.msoy.room.gwt.WebRoomService.MemberRoomsResult;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays this member's rooms.
 */
public class RoomsPanel extends VerticalPanel
{
    public RoomsPanel (int memberId)
    {
        setStyleName("roomsPanel");
        _roomsvc.loadMemberRooms(memberId, new MsoyCallback<MemberRoomsResult>() {
            public void onSuccess (MemberRoomsResult result)
            {
                init(result);
            }
        });
    }

    protected void init (MemberRoomsResult result)
    {
        boolean isOwner = result.owner.getMemberId() == CPeople.getMemberId();
        CPeople.frame.setTitle(isOwner ? _msgs.roomsMineTitle()
            : _msgs.roomsTitle(result.owner.toString()));

        add(new TongueBox(null, isOwner ? _msgs.roomsMineIntro()
            : _msgs.roomsIntro(result.owner.toString()), false));

        SmartTable grid = new SmartTable(0, 0);
        for (int ii = 0; ii < result.rooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            grid.setWidget(row, col, new RoomWidget(result.rooms.get(ii)));
        }
        add(new TongueBox(isOwner ? _msgs.roomsMineTitle()
            : _msgs.roomsTitle(result.owner.toString()), grid));
    }

    protected static class RoomWidget extends SmartTable
    {
        public RoomWidget (final RoomInfo room)
        {
            super("Room", 0, 2);
            ClickListener onClick = Link.createListener(Pages.WORLD, "s"+room.sceneId);
            setWidget(0, 0, MediaUtil.createSceneThumbView(room.thumbnail, onClick));
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 3;
}
