//
// $Id$

package client.room;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;
import client.ui.MsoyUI;
import client.ui.Stars;

public class RoomWidget extends FlowPanel
{
    public RoomWidget (final RoomInfo room)
    {
        setStyleName("Room");

        if (room.winnerRank != null) {
            add(MsoyUI.createLabel(room.winnerRank, "WinnerRank"));
        }

        Widget thumb = SceneUtil.createSceneThumbView(
            room.thumbnail, Link.createListener(Pages.WORLD, "s"+room.sceneId));
        thumb.setTitle(_msgs.rwThumbTip());
        add(thumb);
        Widget name = Link.create(room.name, Pages.ROOMS, Args.compose("room", room.sceneId));
        name.setTitle(_msgs.rwNameTip());
        add(name);
        add(new Stars(room.rating, true, true, null));
        if (room.population > 0) {
            add(MsoyUI.createLabel(_msgs.rwRoomPopulation(""+room.population), null));
        }
    }

    protected static final RoomMessages _msgs = GWT.create(RoomMessages.class);
}
