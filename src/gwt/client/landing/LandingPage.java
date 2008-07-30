//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.data.Invitation;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shell.TrackingCookie;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new LandingPage();
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

        if (action.equals("i") && CShell.isGuest()) {
            // only load their invitation and redirect to the main page if they're not logged in
            String inviteId = args.get(1, "");
            if (CShell.activeInvite != null && CShell.activeInvite.inviteId.equals(inviteId)) {
                Link.go(Page.ME, "");
            } else {
                _membersvc.getInvitation(inviteId, true, new MsoyCallback<Invitation>() {
                    public void onSuccess (Invitation invite) {
                        CShell.activeInvite = invite;
                        Link.go(Page.ME, "");
                    }
                });
            }

        // landing page for creators (a/b test: half see signup, half see info - default is info)
        } else if (action.equals("creators")) {
            _membersvc.getABTestGroup(
                TrackingCookie.get(), "jul08CreatorsLanding", true, new MsoyCallback<Integer>() {
                    public void onSuccess (Integer group) {
                        CShell.frame.closeClient(); // fullscreen
                        if (group == 1) {
                            setContent(_msgs.titleCreators(), new CreatorsSignupPanel(), false);
                        } else if (group == 2) {
                            setContent(_msgs.titleCreators(), new CreatorsLinksPanel(), false);
                        } else if (group == 3) {
                            Link.go(Page.ME, "");
                        } else {
                            // group 4, and if test is not running visitors see info page
                            setContent(_msgs.titleCreators(), new CreatorsPanel(), false);
                        }
                    }
            });

        // registration form ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorssignuptest")) {
            setContent(_msgs.titleCreators(), new CreatorsSignupPanel(), false);

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsinfotest")) {
            setContent(_msgs.titleCreators(), new CreatorsPanel(), false);

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorslinkstest")) {
            setContent(_msgs.titleCreators(), new CreatorsLinksPanel(), false);

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsoldlandingtest")) {
            Link.go(Page.ME, "");

        } else {
            CShell.frame.closeClient(); // no client on the main guest landing page
            setContent(_msgs.landingTitle(), new LandingPanel(), false);
        }
    }

    @Override
    public String getPageId ()
    {
        return LANDING;
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);
}
