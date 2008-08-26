//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;

/**
 * Displays a profile's "portal" page with their profile information, friends,
 * and whatever else they want showing on their page.
 */
public class PeoplePage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("search")) {
            displaySearch(args);

        } else if (args.get(0, 0) != 0) {
            setContent(CPeople.msgs.profileTitle(), new ProfilePanel(args.get(0, 0)));

        } else if (action.equals("f")) {
            setContent(new FriendsPanel(args.get(1, 0)));

        } else if (action.equals("rooms")) {
            setContent(new RoomsPanel(args.get(1, 0)));

        } else if (CPeople.isGuest()) {
            setContent(new PeoplePanel());

        } else if (action.equals("me")) { // !guest
            setContent(new ProfilePanel(CPeople.getMemberId()));

        } else if (action.equals("invites")) { // !guest
            setContent(CPeople.msgs.inviteTitle(), new InvitePanel());

        } else { // !guest
            setContent(new FriendsPanel(CPeople.getMemberId()));
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.PEOPLE;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CPeople.msgs = (PeopleMessages)GWT.create(PeopleMessages.class);
    }

    protected void displaySearch (Args args)
    {
        if (_search == null) {
            _search = new SearchPanel();
        }
        _search.setArgs(args);
        setContent(_search);
    }

    protected int _memberId = -1;
    protected SearchPanel _search;
}
