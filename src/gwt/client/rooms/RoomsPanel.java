//
// $Id$

package client.rooms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.room.RoomWidget;
import client.ui.Marquee;
import client.ui.MsoyUI;
import client.ui.StretchButton;
import client.ui.TongueBox;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class RoomsPanel extends FlowPanel
{
    public RoomsPanel ()
    {
        setStyleName("roomsPanel");

        SmartTable header = new SmartTable("Header", 0, 0);
        header.setWidget(0, 0, new Marquee(null, _msgs.roomsMarquee()), 1, null);
        header.setText(1, 0, _msgs.roomsIntro(), 1, "Intro");

        if (FlashClients.clientExists()) {
            StretchButton button = new StretchButton(StretchButton.ORANGE_THICK, "Start Whirled Tour",
                new ClickListener () {
                public void onClick (Widget sender) {
                    FlashClients.startTour();
                }
            });
            header.setWidget(0, 1, button, 1, "Tour");
            header.getFlexCellFormatter().setRowSpan(0, 1, 2);
            header.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        }

        add(header);

        _worldsvc.loadOverview(new MsoyCallback<WebRoomService.OverviewResult>() {
            public void onSuccess (WebRoomService.OverviewResult overview) {
                init(overview);
            }
        });
    }

    protected void init (WebRoomService.OverviewResult overview)
    {

        RoomsGrid active = new RoomsGrid();
        active.setModel(new SimpleDataModel<RoomInfo>(overview.activeRooms), 0);
        add(new TongueBox(_msgs.activeRooms(), active));

        RoomsGrid cool = new RoomsGrid();
        cool.setModel(new SimpleDataModel<RoomInfo>(overview.coolRooms), 0);
        add(new TongueBox(_msgs.coolRooms(), cool));
    }

    protected static class RoomsGrid extends PagedGrid<RoomInfo>
    {
        public RoomsGrid ()
        {
            super(2, 3, NAV_ON_BOTTOM);
        }

        @Override // from PagedGrid
        protected Widget createWidget (RoomInfo room)
        {
            return new RoomWidget(room);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return _msgs.emptyGrid(); // This should almost never happen
        }
    }

    protected static final RoomsMessages _msgs = GWT.create(RoomsMessages.class);
    protected static final WebRoomServiceAsync _worldsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
