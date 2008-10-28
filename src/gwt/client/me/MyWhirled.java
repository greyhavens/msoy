//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.web.gwt.Pages;

import client.person.FeedPanel;
import client.person.PersonMessages;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class MyWhirled extends FlowPanel
{
    public MyWhirled ()
    {
        setStyleName("myWhirled");

        // add an additional links only on this page; this will hopefully some day move into the
        // main interface on this page
        CShell.frame.addNavLink("My Profile", Pages.PEOPLE, "" + CShell.getMemberId(), 1);

        _mesvc.getMyWhirled(new MsoyCallback<MyWhirledData>() {
            public void onSuccess (MyWhirledData data) {
                init(data);
            }
        });
    }

    protected void init (MyWhirledData data)
    {
        add(MsoyUI.createLabel(_msgs.populationDisplay("" + data.whirledPopulation),
            "PeopleOnline"));

        Image daContestBanner = MsoyUI.createActionImage(
            "/images/landing/dacontest_me_banner.jpg",
            Link.createListener(Pages.ME, MePage.DEVIANT_CONTEST_IFRAME));
        daContestBanner.addStyleName("DAContestBanner");
        add(daContestBanner);

        String empty = data.friendCount > 0 ? _pmsgs.emptyFeed() : _pmsgs.emptyFeedNoFriends();
        FeedPanel feed = new FeedPanel(empty, new FeedPanel.FeedLoader() {
            public void loadFeed (int feedDays, AsyncCallback<List<FeedMessage>> callback) {
                _mesvc.loadFeed(feedDays, callback);
            }
        });
        feed.setFeed(data.feed, false);
        FlowPanel feedBox = MsoyUI.createFlowPanel("FeedBox");
        feedBox.add(new Image("/images/me/me_feed_topcorners.png"));
        feedBox.add(MsoyUI.createLabel(_msgs.newsTitle(), "NewsTitle"));
        feedBox.add(feed);
        feedBox.add(new Image("/images/me/me_feed_bottomcorners.png"));

        // news feed on the left, friends on the right
        FloatPanel newsAndFriends = new FloatPanel("NewsAndFriends");
        add(newsAndFriends);
        newsAndFriends.add(feedBox);
        newsAndFriends.add(new MeFriendsPanel(data));
    }

    protected static final MeMessages _msgs = (MeMessages)GWT.create(MeMessages.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
