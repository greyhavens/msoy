//
// $Id$

package client.whirleds;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.ui.TongueBox;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays the rooms of a particular Whirled.
 */
public class WhirledRoomsPanel extends VerticalPanel
{
    public WhirledRoomsPanel (GroupDetail detail)
    {
        _detail = detail;

        _roomsvc.loadGroupRooms(
            _detail.group.groupId, new MsoyCallback<WebRoomService.RoomsResult>() {
            public void onSuccess (WebRoomService.RoomsResult result) {
                init(result);
            }
        });
    }

    protected void init (WebRoomService.RoomsResult rooms)
    {
        _myRooms = rooms.callerRooms;
        add(new TongueBox(null, _msgs.detailRoomsDetail(_detail.group.name), false));
        _roomsGrid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.groupRooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            _roomsGrid.setWidget(row, col, new RoomWidget(rooms.groupRooms.get(ii)));
        }
        add(new TongueBox(_msgs.detailRoomsTitle(_detail.group.name), _roomsGrid));

        VerticalPanel transferPanel = new VerticalPanel();
        transferPanel.add(new Label(_msgs.detailTransferRoomInfo()));
        HorizontalPanel transferForm = new HorizontalPanel();
        transferPanel.add(transferForm);
        transferForm.setSpacing(10);
        transferForm.add(_roomsListBox = new ListBox());
        for (RoomInfo callerRoom : _myRooms) {
            _roomsListBox.addItem(callerRoom.name);
        }
        Button transferButton = new Button(_msgs.detailTransferRoom(_detail.group.name),
            new ClickListener() {
                public void onClick (Widget sender) {
                    transferCurrentRoom();
                }
            });
        transferForm.add(transferButton);
        add(new TongueBox(_msgs.detailCallersRoomsTitle(), transferPanel));
    }

    protected void transferCurrentRoom ()
    {
        final int index = _roomsListBox.getSelectedIndex();
        if (index < 0) {
            return;
        }
        RoomInfo room = _myRooms.get(index);
        _groupsvc.transferRoom(_detail.group.groupId, room.sceneId, new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                moveSceneToGrid(index);
            }
        });
    }

    protected void moveSceneToGrid (int index)
    {
        // TODO if we leave this tab and come back to it, this data should be refreshed from the
        // server
        RoomInfo room = _myRooms.remove(index);
        _roomsListBox.removeItem(index);
        int row = _roomsGrid.getRowCount() - 1;
        int col = _roomsGrid.getCellCount(row);
        if (col >= ROOM_COLUMNS) {
            row++;
            col = 0;
        } else {
            col++;
        }
        _roomsGrid.setWidget(row, col, new RoomWidget(room));
    }

    protected static class RoomWidget extends SmartTable
    {
        public RoomWidget (RoomInfo room)
        {
            super("Room", 0, 2);
            ClickListener onClick = Link.createListener(Pages.WORLD, "s"+room.sceneId);
            setWidget(0, 0, MediaUtil.createSceneThumbView(room.canonicalThumbnail, onClick));
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
            getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
        }
    }

    protected GroupDetail _detail;
    protected List<RoomInfo> _myRooms;
    protected ListBox _roomsListBox;
    protected SmartTable _roomsGrid;

    protected static final WhirledsMessages _msgs = GWT.create(WhirledsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);
    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 2;
}
