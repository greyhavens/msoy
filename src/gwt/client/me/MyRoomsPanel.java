//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.WorldService;

import client.shell.Application;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;
import client.util.TongueBox;

/**
 * Displays this member's rooms.
 */
public class MyRoomsPanel extends VerticalPanel
{
    public MyRoomsPanel ()
    {
        setStyleName("myRooms");

        CMe.worldsvc.loadMyRooms(CMe.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((List)result);
            }
        });
    }

    protected void init (List rooms)
    {
        add(new TongueBox(null, CMe.msgs.roomsIntro(), false));
        SmartTable grid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            grid.setWidget(row, col, new RoomWidget((WorldService.Room)rooms.get(ii)));
        }
        add(new TongueBox(CMe.msgs.titleRooms(), grid));
    }

    protected static class RoomWidget extends SmartTable
    {
        public RoomWidget (final WorldService.Room room)
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
}
