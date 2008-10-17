//
// $Id$

package client.landing;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Landing page for the Deviant Art "Design Your Whirled" contest. Most content is in images, and
 * many contain English text at fixed sizes.
 */
public class DAContestPanel extends SimplePanel
{
    public DAContestPanel ()
    {
        setStyleName("daContestPanel");
        AbsolutePanel content = MsoyUI.createAbsolutePanel("Content");
        setWidget(content);

        // design your whirled header
        content.add(new Image("/images/landing/dacontest_header.png"));

        // rooms list floats over on the right
        FlowPanel rooms = MsoyUI.createFlowPanel("Rooms");
        content.add(rooms, 660, 135);
        rooms.add(new Image("/images/landing/dacontest_rooms_top.png"));
        FlowPanel roomsList = MsoyUI.createFlowPanel("RoomsList");
        rooms.add(roomsList);
        for (int ii = 0; ii < COOL_ROOM_IDS.length; ii++) {
            int sceneId = COOL_ROOM_IDS[ii];
            String roomName = COOL_ROOM_NAMES[ii];
            String roomImageURL = "/images/landing/dacontest_room_" + sceneId + ".png";
            ClickListener onClick = Link.createListener(Pages.WORLD, "s" + sceneId);
            roomsList.add(MsoyUI.createImage(roomImageURL, "RoomImage"));
            roomsList.add(MsoyUI.createActionLabel(roomName, "RoomName", onClick));
        }
        rooms.add(new Image("/images/landing/dacontest_rooms_bottom.png"));

        // TODO make anything else in this panel a link or text?
        AbsolutePanel howToEnter = MsoyUI.createAbsolutePanel("HowToEnter");
        content.add(howToEnter);
        howToEnter.add(MsoyUI.createButton(MsoyUI.LONG_THICK, "Join Whirled!",
            Link.createListener(Pages.ACCOUNT, "create")), 455, 370);

        AbsolutePanel step1 = MsoyUI.createAbsolutePanel("Step1");
        content.add(step1);
        // TODO Make text and links ActionLabels when content is finalized
        step1.add(createInvisiLink(Link.createListener(Pages.SHOP, "3"), 60, 30), 550, 130);
        step1.add(createInvisiLink(new ClickListener() {
            public void onClick (Widget sender) {
                Window.open("http://wiki.whirled.com/", "_blank", null);
            }
        }, 100, 30), 460, 150);

        AbsolutePanel step2 = MsoyUI.createAbsolutePanel("Step2");
        content.add(step2);

        AbsolutePanel step3 = MsoyUI.createAbsolutePanel("Step3");
        content.add(step3);

        AbsolutePanel rules = MsoyUI.createAbsolutePanel("Rules");
        content.add(rules);
        // TODO make a bigger button as in mockup
        rules.add(MsoyUI.createButton(MsoyUI.LONG_THICK, "Join Whirled!", Link.createListener(
            Pages.ACCOUNT, "create")), 570, 220);
        rules.add(createInvisiLink(new ClickListener() {
            public void onClick (Widget sender) {
                Window.open("http://wiki.whirled.com/", "_blank", null);
            }
        }, 70, 30), 110, 280);
    }

    /**
     * Helper function for creating an invisible area of a given size with a given click event,
     * used to create a sort of imagemap.
     */
    protected Widget createInvisiLink (ClickListener listener, int width, int height)
    {
        Image image = MsoyUI.createActionImage("/images/landing/dacontest_blank.png", listener);
        image.setWidth(width + "px");
        image.setHeight(height + "px");
        return image;
    }

    /** Scenes to display under "check out these cool rooms!"; images are indexed by id */
    protected static final int[] COOL_ROOM_IDS = { 1, 2, 3 };
    protected static final String[] COOL_ROOM_NAMES = { "OOO Tentacles", "Club Bella",
        "Corpse Craft" };
}
