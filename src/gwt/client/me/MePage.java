//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import client.util.FlashClients;
import client.util.Link;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;

public class MePage extends Page
{
    @Override // from Page
    public void onPageLoad ()
    {
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals("account")) {
            setContent(_msgs.titleAccount(), new EditAccountPanel());

        } else if (action.equals("rooms")) {
            setContent(_msgs.titleRooms(), new MyRoomsPanel());

        } else if (action.equals("passport")) {
            setContent(_msgs.titlePassport(), new PassportPanel());

        } else if (!CMe.isGuest()) {
            setContent(new MyWhirled());
            FlashClients.tutorialEvent("myWhirledVisited");

        } else {
            Link.go(null, ""); // redirect to landing page
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ME;
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
