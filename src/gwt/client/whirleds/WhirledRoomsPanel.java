//
// $Id$

package client.whirleds;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.group.data.GroupDetail;

import com.threerings.msoy.web.client.GroupService;

import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;
import client.util.TongueBox;

import client.shell.Application;
import client.shell.Page;

/**
 * Displays the rooms of a particular Whirled. 
 */
public class WhirledRoomsPanel extends VerticalPanel
{
    public WhirledRoomsPanel (GroupDetail detail)
    {
        _detail = detail;
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _loaded) {
            return;
        }

        clear();

        CWhirleds.groupsvc.getGroupRooms(
            CWhirleds.ident, _detail.group.groupId, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((GroupService.RoomsResult)result);
            }
        });
        _loaded = true; // note that we've asked for our data
    }

    protected void init (GroupService.RoomsResult rooms)
    {
        _roomsResult = rooms;
        add(new TongueBox(null, CWhirleds.msgs.detailRoomsDetail(_detail.group.name), false));
        _roomsGrid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.groupRooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            _roomsGrid.setWidget(row, col, 
                new RoomWidget((GroupService.Room)rooms.groupRooms.get(ii)));
        }
        add(new TongueBox(CWhirleds.msgs.detailRoomsTitle(_detail.group.name), _roomsGrid));

        HorizontalPanel transferPanel = new HorizontalPanel();
        transferPanel.setSpacing(10);
        transferPanel.add(_roomsListBox = new ListBox());
        for (int ii = 0; ii < _roomsResult.callerRooms.size(); ii++) {
            _roomsListBox.addItem(((GroupService.Room)_roomsResult.callerRooms.get(ii)).name);
        }
        Button transferButton = new Button(CWhirleds.msgs.detailTransferRoom(_detail.group.name),
            new ClickListener() {
                public void onClick (Widget sender) {
                    transferCurrentRoom();
                }
            });
        transferPanel.add(transferButton);
        add(new TongueBox(CWhirleds.msgs.detailCallersRoomsTitle(), transferPanel));
    }

    protected void transferCurrentRoom () 
    {
        final int index = _roomsListBox.getSelectedIndex();
        if (index < 0) {
            return;
        }
        GroupService.Room room = (GroupService.Room)_roomsResult.callerRooms.get(index);
        CWhirleds.groupsvc.transferRoom(CWhirleds.ident, _detail.group.groupId, room.sceneId,
            new MsoyCallback() {
                public void onSuccess (Object result) {
                    moveSceneToGrid(index);
                }
            });
    }

    protected void moveSceneToGrid (int index)
    {
        // if we leave this tab and come back to it, this data should be refreshed from the server
        _loaded = false;

        GroupService.Room room = (GroupService.Room)_roomsResult.callerRooms.remove(index);
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
            ClickListener onClick = Application.createLinkListener(Page.WORLD, "s"+room.sceneId);
            MediaDesc decor = (room.decor != null) ? room.decor :
                Item.getDefaultThumbnailMediaFor(Item.DECOR);
            setWidget(0, 0, new ThumbBox(decor, onClick));
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
        }
    }

    protected static final int ROOM_COLUMNS = 6;

    protected boolean _loaded;
    protected GroupDetail _detail;
    protected GroupService.RoomsResult _roomsResult;
    protected ListBox _roomsListBox;
    protected SmartTable _roomsGrid;
}
