//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;
import com.threerings.msoy.room.gwt.WebRoomService.MemberRoomsResult;

import client.room.RoomWidget;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.Stars;
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
        boolean isOwner = result.owner.getMemberId() == CShell.getMemberId();
        CShell.frame.setTitle(isOwner ? _msgs.roomsMineTitle()
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

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 3;
}
