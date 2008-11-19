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
import client.util.ServiceUtil;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    public static String CREATORS = "creators";
    public static String DEVIANT_CONTEST = "dacontest";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // creators panel won our creators a/b test
        if (action.equals(CREATORS)) {
            setContent(_msgs.titleCreators(), new CreatorsPanel());

        // landing page for deviant art contest
        } else if (action.equals(DEVIANT_CONTEST)) {
            setContent(_msgs.titleDAContest(), new DAContestLandingPanel());

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
