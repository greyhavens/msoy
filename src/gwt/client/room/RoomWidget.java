//
// $Id$

package client.room;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;
import client.ui.MsoyUI;
import client.ui.Stars;

public class RoomWidget extends FlowPanel
{
    /**
     * Create a normal RoomWidget with all the info.
     */
    public RoomWidget (RoomInfo room)
    {
        if (room.winnerRank != null) {
            add(MsoyUI.createLabel(room.winnerRank, "WinnerRank"));
        }
        init(room.sceneId, room.name, room.thumbnail);
        add(new Stars(room.rating, true, true, null));
        if (room.population > 0) {
            add(MsoyUI.createLabel(_msgs.rwRoomPopulation(""+room.population), null));
        }
    }

    /**
     * Create a "plain vanilla" RoomWidget, which is used when mailing the room, etc.
     */
    public RoomWidget (int sceneId, String name, MediaDesc thumbnail)
    {
        init(sceneId, name, thumbnail);
    }

    /**
     * Configure the UI.
     */
    protected void init (int sceneId, String name, MediaDesc thumbnail)
    {
        setStyleName("Room");

        Widget thumb = SceneUtil.createSceneThumbView(
            thumbnail, Link.createListener(Pages.WORLD, "s" + sceneId));
        thumb.setTitle(_msgs.rwThumbTip());
        add(thumb);
        Widget nameLink = Link.create(name, Pages.ROOMS, "room", sceneId);
        nameLink.setTitle(_msgs.rwNameTip());
        add(nameLink);
    }

    protected static final RoomMessages _msgs = GWT.create(RoomMessages.class);
}
