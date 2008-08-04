//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import client.util.FlashClients;
import client.util.Link;

import client.shell.Args;
import client.shell.Page;

public class MePage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new MePage();
            }
        };
    }

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
            Link.go("", ""); // redirect to landing page
        }
    }

    @Override
    public String getPageId ()
    {
        return ME;
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
