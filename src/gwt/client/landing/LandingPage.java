//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.landing.gwt.LandingService;
import com.threerings.msoy.landing.gwt.LandingServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.shell.Page;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    public static String CREATORS = "creators";
    public static String DEVIANT_CONTEST = "dacontest";
    public static String DEVIANT_CONTEST_WINNERS = "dawinners";
    public static String GAME_CONTEST = "gamecontest";
    public static String DESIGN_CONTEST = "designcontest";
    public static String LANDING_COMBINED = "combined";
    public static String LANDING_GAMES = "games";
    public static String LANDING_ROOMS = "rooms";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // creators panel won our creators a/b test
        if (action.equals(CREATORS)) {
            setContent(_msgs.titleCreators(), new CreatorsPanel());

        // landing page for deviant art contest
        } else if (action.equals(DEVIANT_CONTEST)) {
            Link.go(Pages.LANDING, "dawinners");

        // landing page for deviant art contest winners
        } else if (action.equals(DEVIANT_CONTEST_WINNERS)) {
            setContent(_msgs.titleDAContestWinners(), new DAContestWinnersPanel());

        // landing page for flash game developer contest
        } else if (action.equals(GAME_CONTEST)) {
            setContent(_msgs.titleGameContest(), new GameContestPanel());

        // landing page for design your whirled contest
        } else if (action.equals(DESIGN_CONTEST)) {
            setContent(_msgs.titleDesignContest(), new DesignContestPanel());

        // combined (old) landing page
        } else if (action.equals(LANDING_COMBINED)) {
            setContent(_msgs.landingTitle(), new LandingPanel());

        // game-centric landing page
        } else if (action.equals(LANDING_GAMES)) {
            setContent(_msgs.landingTitle(), new GamesLandingPanel());

        // room-centric landing page
        } else if (action.equals(LANDING_ROOMS)) {
            setContent(_msgs.landingTitle(), new RoomsLandingPanel());

        // A/B/C test between combined, game-centric and room-centric landing pages
        } else {
            runABTest();
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.LANDING;
    }

    /**
     * Runs AB test defined on the landing page.
     */
    protected void runABTest ()
    {
        // list of redirects, based on the user's test group.
        final String[] testpages = new String[] {
            // since groups are 1-indexed, we use "group 0" to mean the default value.
            LANDING_COMBINED,
            // our test groups
            LANDING_COMBINED, LANDING_GAMES, LANDING_ROOMS };

        _membersvc.getABTestGroup(CShell.visitor, "2008 11 landing games rooms", true,
            new AsyncCallback<Integer>() {
                public void onSuccess (Integer group) {
                    gotTestGroup(testpages, group);
                }

                public void onFailure (Throwable cause) {
                    gotTestGroup(testpages, -1);
                }
            });
    }

    protected void gotTestGroup (String[] testpages, int group)
    {
        if (group > 0 && group < testpages.length) {
            Link.go(Pages.LANDING, testpages[group]);
        } else {
            Link.go(Pages.LANDING, testpages[0]);
        }
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final LandingServiceAsync _landingsvc = (LandingServiceAsync)
        ServiceUtil.bind(GWT.create(LandingService.class), LandingService.ENTRY_POINT);
}
