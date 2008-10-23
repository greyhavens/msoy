//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;

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

public class MyWhirled extends VerticalPanel
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
        add(MsoyUI.createLabel(_msgs.populationDisplay("" + data.whirledPopulation), "Pop"));

        Image daContestBanner = MsoyUI.createActionImage(
            "/images/landing/dacontest_me_banner.jpg",
            Link.createListener(Pages.ME, MePage.DEVIANT_CONTEST_IFRAME));
        daContestBanner.addStyleName("DAContestBanner");
        add(daContestBanner);

        add(new WhatsNextPanel(data));
        String empty = data.friendCount > 0 ? _pmsgs.emptyFeed() : _pmsgs.emptyFeedNoFriends();
        FeedPanel feed = new FeedPanel(empty, true, new FeedPanel.FeedLoader() {
            public void loadFeed (int feedDays, AsyncCallback<List<FeedMessage>> callback) {
                _mesvc.loadFeed(feedDays, callback);
            }
        });
        feed.setFeed(data.feed, false);
        add(feed);
    }

    protected static final MeMessages _msgs = (MeMessages)GWT.create(MeMessages.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
