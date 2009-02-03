//
// $Id$

package client.rooms;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.room.SceneUtil;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Winning rooms of the Design Your Whirled contest, excluding the first DeviantArt contest.
 * Winner information is hardcoded fer awesome.
 */
public class DesignWinnersPanel extends FlowPanel
{
    public DesignWinnersPanel ()
    {
        setStyleName("designWinnersPanel");

        add(MsoyUI.createSimplePanel(new Image("/images/rooms/design_winners_header.png"),
            "Header"));

        _worldsvc.loadDesignWinners(new MsoyCallback<List<RoomInfo>>() {
            public void onSuccess (List<RoomInfo> winners) {
                init(winners);
            }
        });
    }

    protected void init (List<RoomInfo> winners)
    {
        if (winners.size() < 8) {
            add(MsoyUI.createLabel(_msgs.designwinnersNoWinners(), null));
            return;
        }

        // 1st place stands alone
        AbsolutePanel first = MsoyUI.createAbsolutePanel("Winners");
        first.add(MsoyUI.createActionImage("/images/rooms/design_winners_1st.png",
            _msgs.designwinnersEnter(winners.get(0).name), Link.createListener(Pages.WORLD, "s"
                + winners.get(0).sceneId)), 30, 20);
        add(first);

        // 2nd and 3rd place are side by side
        AbsolutePanel secondthird = MsoyUI.createAbsolutePanel("Winners");
        secondthird.add(MsoyUI.createActionImage("/images/rooms/design_winners_2nd.png",
            _msgs.designwinnersEnter(winners.get(1).name), Link.createListener(Pages.WORLD, "s"
                + winners.get(1).sceneId)), 35, 25);
        secondthird.add(MsoyUI.createActionImage("/images/rooms/design_winners_3rd.png",
            _msgs.designwinnersEnter(winners.get(2).name), Link.createListener(Pages.WORLD, "s"
                + winners.get(2).sceneId)), 325, 25);
        add(secondthird);

        // honorable mentions displayed with snapshot
        FlowPanel honorable = MsoyUI.createFlowPanel("Winners");
        honorable.add(MsoyUI.createLabel(_msgs.designwinnersHonorable(), "Title"));
        honorable.add(MsoyUI.createLabel(_msgs.designwinnersHonorableSub(), "SubTitle"));
        FloatPanel honorableTop = new FloatPanel("HonorableTop");
        honorableTop.add(makeHonorableMention(winners.get(3)));
        honorableTop.add(makeHonorableMention(winners.get(4)));
        honorableTop.add(makeHonorableMention(winners.get(5)));
        honorable.add(honorableTop);
        FloatPanel honorableBottom = new FloatPanel("HonorableBottom");
        honorableBottom.add(makeHonorableMention(winners.get(6)));
        honorableBottom.add(makeHonorableMention(winners.get(7)));
        honorable.add(honorableBottom);
        add(honorable);

        // too cool for school displayed text-only
        FlowPanel cool = MsoyUI.createFlowPanel("Winners");
        cool.add(MsoyUI.createLabel(_msgs.designwinnersTooCool(), "Title"));
        cool.add(MsoyUI.createLabel(_msgs.designwinnersTooCoolSub(), "SubTitle"));
        for (int ii = 8; ii < winners.size(); ii++) {
            cool.add(makeTooCool(winners.get(ii)));
        }
        add(cool);
    }

    protected Widget makeHonorableMention (RoomInfo room)
    {
        FlowPanel panel = MsoyUI.createFlowPanel("HonorableMention");
        Widget thumb = SceneUtil.createSceneThumbView(room.thumbnail, Link.createListener(
            Pages.WORLD, "s" + room.sceneId));
        panel.add(thumb);
        Widget name = Link.create(room.name, Pages.ROOMS, Args.compose("room", room.sceneId));
        panel.add(name);
        return panel;
    }

    protected Widget makeTooCool (RoomInfo room)
    {
        FlowPanel panel = MsoyUI.createFlowPanel("TooCool");
        Widget name = Link.create(room.name, Pages.WORLD, "s" + room.sceneId);
        panel.add(name);
        return panel;
    }

    protected static final RoomsMessages _msgs = GWT.create(RoomsMessages.class);
    protected static final WebRoomServiceAsync _worldsvc = (WebRoomServiceAsync)ServiceUtil.bind(
        GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
