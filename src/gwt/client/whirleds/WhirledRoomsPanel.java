//
// $Id$

package client.whirleds;

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
import com.threerings.msoy.item.data.all.Item;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.ui.TongueBox;
import client.util.Link;
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

        _groupsvc.getGroupRooms(_detail.group.groupId, new MsoyCallback<GroupService.RoomsResult>() {
            public void onSuccess (GroupService.RoomsResult result) {
                init(result);
            }
        });
    }

    protected void init (GroupService.RoomsResult rooms)
    {
        _roomsResult = rooms;
        add(new TongueBox(null, _msgs.detailRoomsDetail(_detail.group.name), false));
        _roomsGrid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.groupRooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            _roomsGrid.setWidget(row, col,
                new RoomWidget(rooms.groupRooms.get(ii)));
        }
        add(new TongueBox(_msgs.detailRoomsTitle(_detail.group.name), _roomsGrid));

        VerticalPanel transferPanel = new VerticalPanel();
        transferPanel.add(new Label(_msgs.detailTransferRoomInfo()));
        HorizontalPanel transferForm = new HorizontalPanel();
        transferPanel.add(transferForm);
        transferForm.setSpacing(10);
        transferForm.add(_roomsListBox = new ListBox());
        for (GroupService.Room callerRoom : _roomsResult.callerRooms) {
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
        GroupService.Room room = _roomsResult.callerRooms.get(index);
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
        GroupService.Room room = _roomsResult.callerRooms.remove(index);
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
        public RoomWidget (GroupService.Room room)
        {
            super("Room", 0, 2);
            ClickListener onClick = Link.createListener(Pages.WORLD, "s"+room.sceneId);
            MediaDesc decor = (room.decor != null) ? room.decor :
                Item.getDefaultThumbnailMediaFor(Item.DECOR);
            setWidget(0, 0, new ThumbBox(decor, onClick));
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
        }
    }

    protected GroupDetail _detail;
    protected GroupService.RoomsResult _roomsResult;
    protected ListBox _roomsListBox;
    protected SmartTable _roomsGrid;

    protected static final WhirledsMessages _msgs = GWT.create(WhirledsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 6;
}
