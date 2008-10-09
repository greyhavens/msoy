//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import client.person.GalleryActions;
import client.person.GalleryEditPanel;
import client.person.GalleryPanel;
import client.person.GalleryViewPanel;
import client.shell.Args;
import client.shell.CShell;
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
            setContent(_msgs.profileTitle(), new ProfilePanel(args.get(0, 0)));

        } else if (action.equals("f")) {
            setContent(new FriendsPanel(args.get(1, 0)));

        } else if (action.equals("rooms")) {
            setContent(new RoomsPanel(args.get(1, 0)));

        } else if (action.equals(GalleryActions.GALLERIES)) {
            setContent(_msgs.galleriesTitle(), new GalleryPanel(args.get(1, CShell.getMemberId())));

        } else if (action.equals(GalleryActions.VIEW)) {
            setContent(_msgs.galleriesTitle(), _galleryViewPanel = new GalleryViewPanel());
            _galleryViewPanel.setArgs(args, false);

        } else if (action.equals(GalleryActions.VIEW_PROFILE)) {
            // unlike VIEW_PHOTO_ACTION, this will always refresh the gallery data
            setContent(_msgs.galleriesTitle(), _galleryViewPanel = new GalleryViewPanel());
            _galleryViewPanel.setArgs(args, true);

        } else if (action.equals(GalleryActions.VIEW_PHOTO)) {
            // keep the gallery if one is already there
            if (_galleryViewPanel == null) {
                _galleryViewPanel = new GalleryViewPanel();
            }
            setContent(_msgs.galleriesTitle(), _galleryViewPanel);
            // this will only reload gallery data if a different gallery is being viewed
            _galleryViewPanel.setArgs(args, false);

        } else if (CShell.isGuest()) {
            setContent(new PeoplePanel());

        } else if (action.equals(GalleryActions.CREATE_PROFILE)) { // !guest
            setContent(_msgs.galleriesTitle(), new GalleryEditPanel(true));

        } else if (action.equals(GalleryActions.CREATE)) { // !guest
            setContent(_msgs.galleriesTitle(), new GalleryEditPanel(false));

        } else if (action.equals(GalleryActions.EDIT)) { // !guest
            int galleryId = args.get(1, -1);
            setContent(_msgs.galleriesTitle(), new GalleryEditPanel(galleryId));

        } else if (action.equals("me")) { // !guest
            setContent(new ProfilePanel(CShell.getMemberId()));

        } else if (action.equals("invites")) { // !guest
            setContent(_msgs.inviteTitle(), new InvitePanel());

        } else { // !guest
            setContent(new FriendsPanel(CShell.getMemberId()));
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.PEOPLE;
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
    protected GalleryViewPanel _galleryViewPanel;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
