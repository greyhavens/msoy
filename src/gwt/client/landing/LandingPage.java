//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

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
    public static String CREATORS_PARLOR = "parlor";
    public static String CREATORS_POPULAR = "popular";

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

        // landing page for content creators, redirects to a popular room
        } else if (action.equals(CREATORS_POPULAR)) {
            redirectToPopularWhirled();

        // landing page for content creators, redirects to the parlor
        } else if (action.equals(CREATORS_PARLOR)) {
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
        // list of redirects, based on the user's test group.
        // part of the aug08CreatorsLanding2 A/B test - see JIRA WRLD-251.
        final String[] testpages = new String[] {
            // since groups are 1-indexed, we reuse "group 0" to mean the default value.
            CREATORS_INFO,
            // everything else
            CREATORS_LINKS, CREATORS_POPULAR, CREATORS_PARLOR, CREATORS_INFO
        };

        _membersvc.getABTestGroup(
            TrackingCookie.get(), "aug08CreatorsLanding2", true, new AsyncCallback<Integer>() {
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
