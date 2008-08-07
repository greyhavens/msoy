//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.room.gwt.WorldService;
import com.threerings.msoy.room.gwt.WorldServiceAsync;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.ServiceUtil;

/**
 * Handles the MetaSOY main page.
 */
public class WorldPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new WorldPage();
            }
        };
    }

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
        return Pages.WORLD;
    }

    protected static final WorldMessages _msgs = GWT.create(WorldMessages.class);
    protected static final WorldServiceAsync _worldsvc = (WorldServiceAsync)
        ServiceUtil.bind(GWT.create(WorldService.class), WorldService.ENTRY_POINT);
}
