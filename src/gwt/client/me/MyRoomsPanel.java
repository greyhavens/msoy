//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.world.gwt.WorldService;

import client.shell.Page;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.SceneThumbnail;
import client.util.TongueBox;

/**
 * Displays this member's rooms.
 */
public class MyRoomsPanel extends VerticalPanel
{
    public MyRoomsPanel ()
    {
        setStyleName("myRooms");

        CMe.worldsvc.loadMyRooms(CMe.ident, new MsoyCallback<List<WorldService.Room>>() {
            public void onSuccess (List<WorldService.Room> rooms) {
                init(rooms);
            }
        });
    }

    protected void init (List<WorldService.Room> rooms)
    {
        add(new TongueBox(null, CMe.msgs.roomsIntro(), false));
        SmartTable grid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            grid.setWidget(row, col, new RoomWidget(rooms.get(ii)));
        }
        add(new TongueBox(CMe.msgs.titleRooms(), grid));
    }

    protected static class RoomWidget extends SmartTable
    {
        public RoomWidget (final WorldService.Room room)
        {
            super("Room", 0, 2);
            ClickListener onClick = Link.createListener(Page.WORLD, "s"+room.sceneId);
            SceneThumbnail sceneImage = new SceneThumbnail(room.sceneId);
            sceneImage.addClickListener(onClick);
            setWidget(0, 0, sceneImage);
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
        }
    }

    protected static final int ROOM_COLUMNS = 1;
}
