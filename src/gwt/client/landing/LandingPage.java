//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.landing.gwt.LandingData;
import com.threerings.msoy.landing.gwt.LandingService;
import com.threerings.msoy.landing.gwt.LandingServiceAsync;
import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.client.WebMemberServiceAsync;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;
import client.shell.TrackingCookie;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    public static String CREATORS_MAIN = "creators";
    public static String CREATORS_INFO = "creatorsinfo";
    public static String CREATORS_LINKS = "creatorslinks";

    @Override // from Page
    public void onPageLoad ()
    {
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // general landing page for creators runs our custom A/B tests
        if (action.equals(CREATORS_MAIN)) {
            runABTests();

        // landing page for content creators
        } else if (action.equals(CREATORS_INFO)) {
            setContent(_msgs.titleCreators(), new CreatorsPanel());

        // landing page for content creators, with a multitude of links
        } else if (action.equals(CREATORS_LINKS)) {
            setContent(_msgs.titleCreators(), new CreatorsLinksPanel());

        // some test redirects
        //
        // redirect to some popular whirled (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorswhirleds")) {
            redirectToPopularWhirled();

        // redirect to some popular whirled (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsrooms")) {
            redirectToStoryRooms();

        } else {
            setContent(_msgs.landingTitle(), new LandingPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.LANDING;
    }

    /**
     * Runs AB tests defined on the landing page.
     */
    protected void runABTests ()
    {
        _membersvc.getABTestGroup(
            TrackingCookie.get(), "aug08CreatorsLanding2", true, new MsoyCallback<Integer>() {
                public void onSuccess (Integer group) {
                    switch (group) {
                    case 1:
                        Link.go(Pages.LANDING, CREATORS_LINKS);
                        break;
                    case 2:
                        redirectToPopularWhirled();
                        break;
                    case 3:
                        redirectToStoryRooms();
                        break;
                    default:
                        // group 4, and if test is not running visitors see info page
                        Link.go(Pages.LANDING, CREATORS_INFO);
                    }
                }});
    }

    /**
     * Redirects the viewer to a popular Whirled, or to Brave New Whirled if
     * a suitable whirled is not available.
     *
     * Part of the aug08CreatorsLanding2 A/B test. See JIRA WRLD-251.
     *
     * TODO: remove me after the test has ended.
     */
    protected void redirectToPopularWhirled ()
    {
        // population min and max for determining suitable whirled
        final int min = 2;
        final int max = 15;

        _landingsvc.getLandingData(new MsoyCallback<LandingData>() {
            public void onSuccess (LandingData data) {
                // take people to Brave New Whirled by default
                int target = 1;
                // but maybe there's a better place for them
                for (GroupCard card : data.featuredWhirleds) {
                    if (card.population > min && card.population < max) {
                        target = card.homeSceneId;
                    }
                }

                Link.go(Pages.WORLD, "s"+target);
            }
        });
    }

    /**
     * Redirects the viewer to a set of story rooms designed specifically for this test.
     *
     * Part of the aug08CreatorsLanding2 A/B test. See JIRA WRLD-251.
     *
     * TODO: remove me after the test has ended.
     */
    protected void redirectToStoryRooms ()
    {
        Link.go(Pages.WORLD, "s57218");
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final LandingServiceAsync _landingsvc = (LandingServiceAsync)
        ServiceUtil.bind(GWT.create(LandingService.class), LandingService.ENTRY_POINT);
}
