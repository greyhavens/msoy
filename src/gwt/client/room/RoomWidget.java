//
// $Id$

package client.room;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;
import client.util.MediaUtil;
import client.ui.MsoyUI;
import client.ui.Stars;

public class RoomWidget extends FlowPanel
{
    public RoomWidget (final RoomInfo room)
    {
        setStyleName("Room");

        ClickListener onClick = Link.createListener(Pages.WORLD, "s"+room.sceneId);
        add(MediaUtil.createSceneThumbView(room.thumbnail, onClick));
        add(MsoyUI.createActionLabel(room.name, onClick));
        add(new Stars(room.rating, true, true, null));
        if (room.population > 0) {
            add(MsoyUI.createLabel(_msgs.roomPopulation(""+room.population), null));
        }
    }

    protected static final RoomMessages _msgs = GWT.create(RoomMessages.class);
}

