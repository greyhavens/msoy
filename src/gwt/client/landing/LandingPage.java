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
    @Override // from Page
    public void onPageLoad ()
    {
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // landing page for creators (a/b test: half see signup, half see info - default is info)
        if (action.equals("creators")) {
            _membersvc.getABTestGroup(
                TrackingCookie.get(), "aug08CreatorsLanding", true, new MsoyCallback<Integer>() {
                    public void onSuccess (Integer group) {
                        if (group == 1) {
                            setContent(_msgs.titleCreators(), new CreatorsSignupPanel());
                        } else if (group == 2) {
                            setContent(_msgs.titleCreators(), new CreatorsLinksPanel());
                        } else if (group == 3) {
                            Link.go(Pages.ME, "");
                        } else if (group == 4) {
                            redirectToPopularWhirled();
                        } else {
                            // group 5, and if test is not running visitors see info page
                            setContent(_msgs.titleCreators(), new CreatorsPanel());
                        }
                    }
            });

        // registration form ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorssignuptest")) {
            setContent(_msgs.titleCreators(), new CreatorsSignupPanel());

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsinfotest")) {
            setContent(_msgs.titleCreators(), new CreatorsPanel());

        // info ver of creators landing test (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorslinkstest")) {
            setContent(_msgs.titleCreators(), new CreatorsLinksPanel());

        // redirect to some popular whirled (TODO: FOR TESTING, DO NOT LINK)
        } else if (action.equals("creatorsredirect")) {
            redirectToPopularWhirled();

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
     * Redirects the viewer to a popular Whirled, or to Brave New Whirled if
     * a suitable whirled is not available.
     *
     * Part of the aug08CreatorsLanding A/B test. See JIRA WRLD-251.
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

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final LandingServiceAsync _landingsvc = (LandingServiceAsync)
        ServiceUtil.bind(GWT.create(LandingService.class), LandingService.ENTRY_POINT);
}
