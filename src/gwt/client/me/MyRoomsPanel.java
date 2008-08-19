//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

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
public class MyRoomsPanel extends VerticalPanel
{
    public MyRoomsPanel ()
    {
        setStyleName("myRooms");

        _roomsvc.loadMyRooms(new MsoyCallback<List<RoomInfo>>() {
            public void onSuccess (List<RoomInfo> rooms) {
                init(rooms);
            }
        });
    }

    protected void init (List<RoomInfo> rooms)
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
        public RoomWidget (final RoomInfo room)
        {
            super("Room", 0, 2);
            ClickListener onClick = Link.createListener(Pages.WORLD, "s"+room.sceneId);
            Widget sceneImage = makeThumbnailWidget(room, onClick);
            setWidget(0, 0, sceneImage);
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            setWidget(1, 0, MsoyUI.createActionLabel(room.name, onClick));
        }

        protected Widget makeThumbnailWidget (RoomInfo room, ClickListener onClick) {
            if (room.canonicalThumbnail != null) {
                return MediaUtil.createMediaView(room.canonicalThumbnail,
                    MediaDesc.CANONICAL_IMAGE_SIZE, onClick);
            } else {
                Image image = new Image();
                image.addClickListener(onClick);
                image.setUrl(DEFAULT_HALFSIZE);
                return image;
            }
        }
    }

    protected static final String DEFAULT_HALFSIZE = DeploymentConfig.staticMediaURL
        + "snapshot/default_t.jpg";

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 3;
}
