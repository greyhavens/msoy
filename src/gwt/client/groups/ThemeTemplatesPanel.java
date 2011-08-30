//
// $Id: GroupRoomsPanel.java 18445 2009-10-19 19:47:18Z jamie $

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.room.RoomWidget;
import client.ui.TongueBox;
import client.util.InfoCallback;

/**
 * Displays the template rooms of a particular theme.
 */
public class ThemeTemplatesPanel extends VerticalPanel
{
    public ThemeTemplatesPanel (GroupDetail detail)
    {
        _detail = detail;

        _roomsvc.loadThemeTemplates(
            _detail.group.groupId, new InfoCallback<WebRoomService.TemplatesResult>() {
            public void onSuccess (WebRoomService.TemplatesResult result) {
                init(result);
            }
        });
    }

    protected void init (WebRoomService.TemplatesResult rooms)
    {
        add(new TongueBox(null, _msgs.detailTemplatesDetail(_detail.group.name), false));
        _roomsGrid = new SmartTable("RoomsTable", 0, 0);
        for (int ii = 0; ii < rooms.groupRooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            _roomsGrid.cell(row, col).widget(new RoomWidget(rooms.groupRooms.get(ii))).alignTop();
        }
        add(new TongueBox(_msgs.detailTemplatesTitle(_detail.group.name), _roomsGrid));
    }

    protected GroupDetail _detail;
    protected SmartTable _roomsGrid;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = GWT.create(WebRoomService.class);

    protected static final int ROOM_COLUMNS = 2;
}
