//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.world.gwt.WorldService;
import com.threerings.msoy.world.gwt.WorldServiceAsync;

import client.games.CGames;
import client.msgs.MsgsEntryPoint;
import client.shell.Args;
import client.shell.Page;
import client.shell.TrackingCookie;
import client.util.FlashClients;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
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
            setContent(CMe.msgs.titleAccount(), new EditAccountPanel());

        } else if (action.equals("rooms")) {
            setContent(CMe.msgs.titleRooms(), new MyRoomsPanel());

        } else if (action.equals("i") && CMe.isGuest()) {
            // only load their invitation and redirect to the main page if they're not logged in
            String inviteId = args.get(1, "");
            if (CMe.activeInvite != null && CMe.activeInvite.inviteId.equals(inviteId)) {
                Link.go(Page.ME, "");
            } else {
                _membersvc.getInvitation(inviteId, true, new MsoyCallback<Invitation>() {
                    public void onSuccess (Invitation invite) {
                        CMe.activeInvite = invite;
                        Link.go(Page.ME, "");
                    }
                });
            }

        // landing page for creators (a/b test: half see signup, half see info - default is info)
        } else if (action.equals("creators")) {
            _membersvc.getABTestGroup(
                TrackingCookie.get(), "jul08CreatorsLanding", true, new MsoyCallback<Integer>() {
                    public void onSuccess (Integer group) {
                        CMe.frame.closeClient(false); // fullscreen
                        if (group == 1) {
                            setContent(CMe.msgs.titleCreators(), new CreatorsSignupPanel(), false);
                        } else if (group == 2) {
                            setContent(CMe.msgs.titleCreators(), new CreatorsLinksPanel(), false);
                        } else if (group == 3) {
                            Link.go(Page.ME, "");
                        } else {
                            // group 4, and if test is not running visitors see info page
                            setContent(CMe.msgs.titleCreators(), new CreatorsPanel(), false);
                        }
                    }
            });

        // registration form ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorssignuptest")) {
            setContent(CMe.msgs.titleCreators(), new CreatorsSignupPanel(), false);

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsinfotest")) {
            setContent(CMe.msgs.titleCreators(), new CreatorsPanel(), false);

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorslinkstest")) {
            setContent(CMe.msgs.titleCreators(), new CreatorsLinksPanel(), false);

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsoldlandingtest")) {
            Link.go(Page.ME, "");

        } else if (!CMe.isGuest()) {
            setContent(new MyWhirled());
            FlashClients.tutorialEvent("myWhirledVisited");

        } else {
            displayWhat();
        }
    }

    @Override // from Page
    public void didLogon (WebCreds creds)
    {
        Link.go(ME, "");
    }

    @Override
    public String getPageId ()
    {
        return ME;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CMe.worldsvc = (WorldServiceAsync)GWT.create(WorldService.class);
        ((ServiceDefTarget)CMe.worldsvc).setServiceEntryPoint("/worldsvc");

        // wire up our remote services
        CGames.gamesvc = (GameServiceAsync)GWT.create(GameService.class);
        ((ServiceDefTarget)CGames.gamesvc).setServiceEntryPoint("/gamesvc");

        // load up our translation dictionaries
        CMe.msgs = (MeMessages)GWT.create(MeMessages.class);
    }

    protected void displayWhat ()
    {
        CMe.frame.closeClient(false); // no client on the main guest landing page
        setContent(CMe.msgs.landingTitle(), new LandingPanel(), false);
    }

    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);
}
