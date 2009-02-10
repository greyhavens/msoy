//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;
import com.threerings.msoy.room.gwt.WebRoomService.MemberRoomsResult;

import client.room.RoomWidget;
import client.shell.CShell;
import client.ui.TongueBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays this member's rooms.
 */
public class RoomsPanel extends FlowPanel
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
        CShell.frame.setTitle(isOwner ? _msgs.roomsMineTitle() :
                              _msgs.roomsTitle(result.owner.toString()));

        SmartTable header = new SmartTable(0, 0);
        header.setWidth("100%");
        String intro = isOwner ? _msgs.roomsMineIntro() : _msgs.roomsIntro(result.owner.toString());
        header.setText(0, 0, intro, 1, "TContent");
        if (isOwner) {
            // set up some business for purchasing a room
            AsyncCallback<RoomInfo> boughtCallback = new AsyncCallback<RoomInfo>() {
                public void onFailure (Throwable cause) {} // unused
                public void onSuccess (RoomInfo result) {
                    addRoom(result);
                }
            };
            RoomBuyPanel buyPanel = new RoomBuyPanel();
            buyPanel.init(result.newRoomQuote, boughtCallback);

            header.setWidget(0, 1, buyPanel.createPromptHost(_msgs.buyNewRoom()), 1, "TContent");
        }
        add(new TongueBox(null, header));

        // TODO: this should probably be a paged list, eh?
        _grid = new SmartTable(0, 0);
        _grid.setWidth("100%");
        for (RoomInfo info : result.rooms) {
            addRoom(info);
        }
        add(new TongueBox(isOwner ? _msgs.roomsMineTitle() :
                          _msgs.roomsTitle(result.owner.toString()), _grid));
    }

    /**
     * Add a room to the table.
     */
    protected void addRoom (RoomInfo info)
    {
        int row = _grid.getRowCount() - 1;
        int col;
        if (row == -1) { // first widget
            row = 0;
            col = 0;

        } else {
            col = _grid.getCellCount(row);
            if (col == ROOM_COLUMNS) {
                row++;
                col = 0;
            }
        }
// alt.impl
//        int row = _count / ROOM_COLUMNS;
//        int col = _count % ROOM_COLUMNS;
        _grid.setWidget(row, col, new RoomWidget(info));
        _grid.getFlexCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_TOP);
//        _count++;
    }

//    protected int _count = 0;

    /** The grid showing the user's rooms. */
    protected SmartTable _grid;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 3;
}
