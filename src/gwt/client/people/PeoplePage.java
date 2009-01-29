//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.person.GalleryActions;
import client.person.GalleryEditPanel;
import client.person.GalleryPanel;
import client.person.GalleryViewPanel;
import client.shell.CShell;
import client.shell.Page;

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
            SearchPanel search = (getContent() instanceof SearchPanel) ?
                (SearchPanel)getContent() : new SearchPanel();
            search.setArgs(args);
            setContent(search);

        } else if (args.get(0, 0) != 0) {
            setContent(_msgs.profileTitle(), new ProfilePanel(args.get(0, 0)));

        } else if (action.equals("f")) {
            setContent(new FriendsPanel(args.get(1, 0)));

        } else if (action.equals("rooms")) {
            setContent(new RoomsPanel(args.get(1, 0)));

        } else if (action.equals(GalleryActions.GALLERIES)) {
            setContent(_msgs.galleriesTitle(), new GalleryPanel(args.get(1, CShell.getMemberId())));

        } else if (action.equals(GalleryActions.VIEW)) {
            GalleryViewPanel panel = new GalleryViewPanel();
            setContent(_msgs.galleriesTitle(), panel);
            panel.setArgs(args, false);

        } else if (action.equals(GalleryActions.VIEW_PROFILE)) {
            // unlike VIEW_PHOTO_ACTION, this will always refresh the gallery data
            GalleryViewPanel panel = new GalleryViewPanel();
            setContent(_msgs.galleriesTitle(), panel);
            panel.setArgs(args, true);

        } else if (action.equals(GalleryActions.VIEW_PHOTO)) {
            // keep the gallery if one is already there
            GalleryViewPanel panel = (getContent() instanceof GalleryViewPanel) ?
                (GalleryViewPanel)getContent() : new GalleryViewPanel();
            // don't reset it as content if it's already set as it needs to do special things when
            // it is really removed from the DOM because it's going away
            if (!panel.isAttached()) {
                setContent(_msgs.galleriesTitle(), panel);
            }
            panel.setArgs(args, false);

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
            boolean justRegistered = args.get(1, "").equals("newuser");
            boolean linksPage = args.get(1, "").equals("links");
            boolean sharePage = args.get(1, "").equals("share");
            if (justRegistered) {
                setContent(_msgs.justRegInviteTitle(), new InvitePanel(true, false, null));

            } else if (linksPage) {
                setContent(_msgs.inviteTitle(), new LinkToWhirledPanel());
            } else if (sharePage) {
                setContent(_msgs.inviteTitle(), new SharePanel(args.get(2, ""), args.get(3, ""),
                    args.get(4, ""), args.get(5, "")));
            } else {
                setContent(_msgs.inviteTitle(), new SharePanel());
            }

        } else if (action.equals("friendly")) { // !guest
            setContent(_msgs.greetersTitle(), new GreeterPanel());

        } else if (action.equals("invitetest") && DeploymentConfig.devDeployment) {
            setContent(_msgs.inviteTitle(), new NewSharePanel(-13, "ABEXGhi283--", "game"));

        } else { // !guest
            setContent(new FriendsPanel(CShell.getMemberId()));
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.PEOPLE;
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
