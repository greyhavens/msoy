//
// $Id$

package client.room;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.ServiceUtil;

/**
 * Handles the MetaSOY main page.
 */
public class RoomPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        try {
            String action = args.get(0, "s1");
            if (action.equals("room")) {
                setContent(new RoomPanel(args.get(1, 0)));

            } else if (CShell.isGuest()) {
                setContent(MsoyUI.createLabel(_msgs.logonForHome(), "infoLabel"));

            } else {
                setContent(MsoyUI.createLabel(_msgs.unknownLocation(), "infoLabel"));
            }

        } catch (NumberFormatException e) {
            MsoyUI.error(_msgs.unknownLocation());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ROOM;
    }

    protected static final RoomMessages _msgs = GWT.create(RoomMessages.class);
    protected static final WebRoomServiceAsync _worldsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
