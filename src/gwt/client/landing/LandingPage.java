//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.landing.gwt.LandingService;
import com.threerings.msoy.landing.gwt.LandingServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

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

        // A/B/C test between combined, game-centric and room-centric landing pages
        } else {
            setContent(_msgs.landingTitle(), new LandingPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.LANDING;
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final LandingServiceAsync _landingsvc = (LandingServiceAsync)
        ServiceUtil.bind(GWT.create(LandingService.class), LandingService.ENTRY_POINT);
}
