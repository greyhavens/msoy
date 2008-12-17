//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Pages;

import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;

/**
 * General landing page featuring rooms only.
 */
public class RoomsLandingPanel extends FlowPanel
{
    public RoomsLandingPanel ()
    {
        // shares style with the gamesLandingPanel
        setStyleName("gridLandingPanel");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        add(content);

        FlowPanel main = MsoyUI.createFlowPanel("Main");
        content.add(main);

        main.add(createHeader());
        main.add(createGrid());

        FlowPanel footer = MsoyUI.createFlowPanel("Footer");
        content.add(footer);
        footer.add(new LandingCopyright());
    }

    protected Widget createHeader ()
    {
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");

        // join now
        final Button joinButton = new Button("", Link.createListener(Pages.ACCOUNT, "create"));
        joinButton.setStyleName("JoinButton");
        MsoyUI.addTrackingListener(joinButton, "landingRooms JoinButtonClicked", null);
        header.add(joinButton, 25, 60);

        // logon box
        final FlowPanel logon = new FlowPanel();
        PushButton logonButton = new PushButton(_msgs.landingLogon());
        logonButton.addStyleName("LogonButton");
        logon.add(new LogonPanel(LogonPanel.Mode.LANDING, logonButton));
        logon.add(logonButton);
        header.add(logon, 10, 0);

        // tagline
        final HTML tagline = MsoyUI.createHTML(_msgs.roomsTagline(), null);
        tagline.setStyleName("Tagline");
        header.add(tagline, 15, 140);

        // intro video with click-to-play button
        final SimplePanel video = MsoyUI.createSimplePanel(null, "Video");
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                // controls skin hardcoded in the swf as /images/landing/landing_movie_skin.swf
                video.setWidget(WidgetUtil.createFlashContainer("preview",
                    "/images/landing/landing_movie.swf", 208, 154, null));
            }
        };
        final Image clickToPlayImage = MsoyUI.createInvisiLink(onClick, 208, 154);
        MsoyUI.addTrackingListener(clickToPlayImage, "landingRooms VideoPlayed", null);
        video.setWidget(clickToPlayImage);
        header.add(video, 455, 10);

        return header;
    }

    protected Widget createGrid ()
    {
        RoundBox grid = new RoundBox(RoundBox.MEDIUM_BLUE);
        grid.addStyleName("Grid");

        SmartTable gridTable = new SmartTable("GridTable", 7, 0);
        gridTable.setWidget(0, 0, createGridItem(0));
        gridTable.setWidget(0, 1, createGridItem(1));
        gridTable.setWidget(0, 2, createGridItem(2));
        gridTable.setWidget(1, 0, createGridItem(3));

        FlowPanel center = MsoyUI.createFlowPanel("Center");
        center.add(MsoyUI.createHTML(_msgs.roomsCenterTitle(), "CenterTitle"));
        center.add(MsoyUI.createHTML(_msgs.roomsCenterText(), "CenterText"));
        gridTable.setWidget(1, 1, center);

        gridTable.setWidget(1, 2, createGridItem(4));
        gridTable.setWidget(2, 0, createGridItem(5));
        gridTable.setWidget(2, 1, createGridItem(6));
        gridTable.setWidget(2, 2, createGridItem(7));
        grid.add(gridTable);

        Image allLink = MsoyUI.createActionImage("/images/landing/rooms_all_rooms.png",
            Link.createListener(Pages.ROOMS, ""));
        MsoyUI.addTrackingListener(allLink, "landingRooms AllClicked", null);
        allLink.addStyleName("AllLink");
        grid.add(allLink);

        return grid;
    }

    protected Widget createGridItem (int itemNum)
    {
        Image item = MsoyUI.createActionImage("/images/landing/rooms_room_" + itemNum + ".png",
            ROOM_NAMES[itemNum], Link.createListener(Pages.WORLD, "s" + ROOM_IDS[itemNum]));
        MsoyUI.addTrackingListener(item, "landingRooms RoomClicked", ROOM_NAMES[itemNum]
            + "");
        return item;
    }

    // production ids and names of the rooms displayed on this page
    protected static final int[] ROOM_IDS = { 107217, 119439, 89294, 94819, 99355, 116771,
        130834, 101315 };
    protected static final String[] ROOM_NAMES = { "Little Red's Forest", "Whirled Train Ride",
        "Little Monsters", "Aquatica Whirled", "Explore", "The Emerald", "Paper Whirled",
        "Serena's Airship" };

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
