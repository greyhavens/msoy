//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.SceneThumbnail;
import client.util.ServiceUtil;

/**
 * Displays this member's rooms.
 */
public class MyRoomsPanel extends VerticalPanel
{
    public MyRoomsPanel ()
    {
        setStyleName("myRooms");

        _mesvc.loadMyRooms(CMe.ident, new MsoyCallback<List<MeService.Room>>() {
            public void onSuccess (List<MeService.Room> rooms) {
                init(rooms);
            }
        });
    }

    protected void init (List<MeService.Room> rooms)
    {
        add(new TongueBox(null, _msgs.roomsIntro(), false));
        SmartTable grid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            grid.setWidget(row, col, new RoomWidget(rooms.get(ii)));
        }
        add(new TongueBox(_msgs.titleRooms(), grid));
    }

    protected static class RoomWidget extends SmartTable
    {
        public RoomWidget (final MeService.Room room)
        {
            super("Room", 0, 2);
            ClickListener onClick = Link.createListener(Pages.WORLD, "s"+room.sceneId);
            SceneThumbnail sceneImage = new SceneThumbnail(room.sceneId, true);
            sceneImage.addClickListener(onClick);
            setWidget(0, 0, sceneImage);
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 3;
}
