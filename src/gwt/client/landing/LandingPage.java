//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Page;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    public static final String CREATORS = "creators";
    public static final String DEVIANT_CONTEST = "dacontest";
    public static final String DEVIANT_CONTEST_WINNERS = "dawinners";
    public static final String GAME_CONTEST = "gamecontest";
    public static final String DESIGN_CONTEST = "designcontest";
    public static final String DEVELOPER_INTRO = "devintro";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // creators panel won our creators a/b test
        if (action.equals(CREATORS)) {
            setContent(_msgs.titleCreators(), new CreatorsPanel());

        // landing page for deviant art contest winners
        } else if (action.equals(DEVIANT_CONTEST) || action.equals(DEVIANT_CONTEST_WINNERS)) {
            setContent(_msgs.titleDAContestWinners(), new DAContestWinnersPanel());

        // landing page for flash game developer contest
        } else if (action.equals(GAME_CONTEST)) {
            setContent(_msgs.titleGameContest(), new GameContestPanel());

        // landing page for design your whirled contest
        } else if (action.equals(DESIGN_CONTEST)) {
            setContent(_msgs.titleDesignContest(), new DesignContestPanel());

        // landing page with an introduction to Whirled for developers
        } else if (action.equals(DEVELOPER_INTRO)) {
            setContent(_msgs.landingTitle(), new DeveloperIntroPanel());

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
}
