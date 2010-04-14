//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.Page;
import client.ui.NoNavPanel;
import client.util.Link;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    public static final String CREATORS = "creators";
    public static final String DEVIANT_CONTEST_WINNERS = "dawinners";
    public static final String GAME_CONTEST = "gamecontest";
    public static final String DESIGN_CONTEST = "designcontest";
    public static final String DEVELOPER_INTRO = "devintro";
    public static final String OLD_BLUE_LANDING = "bluelanding";
    public static final String NEW_MONSTER_LANDING = "monsterlanding";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // creators panel won our creators a/b test
        if (action.equals(CREATORS)) {
            setContent(_msgs.titleCreators(), new CreatorsPanel());

        // landing page for flash game developer contest
        } else if (action.equals(GAME_CONTEST)) {
            setContent(_msgs.titleGameContest(), new GameContestPanel());

        // landing page for design your whirled contest
        } else if (action.equals(DESIGN_CONTEST)) {
            setContent(_msgs.titleDesignContest(), new DesignContestPanel());

        // landing page with an introduction to Whirled for developers
        } else if (action.equals(DEVELOPER_INTRO)) {
            setContent(_msgs.titleLanding(), new DeveloperIntroPanel());

        // landing page with an introduction to Whirled for developers
        } else if (action.equals(OLD_BLUE_LANDING)) {
            setContent(_msgs.titleLanding(), NoNavPanel.makeBlue(new LandingPanel()));

        // landing page with an introduction to Whirled for developers
        } else if (action.equals(NEW_MONSTER_LANDING)) {
            setContent(_msgs.titleLanding(), new LandingMonsterPanel());

        } else {
            // default to the blue landing, which won the A/B test against the MonsterPanel
            // by a tiny fraction (we may revisit this decision later)
            Link.go(Pages.LANDING, OLD_BLUE_LANDING);
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
}
