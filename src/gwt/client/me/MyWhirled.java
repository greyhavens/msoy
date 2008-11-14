//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.web.gwt.Pages;

import client.person.FriendsFeedPanel;
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

        _mesvc.getMyWhirled(new MsoyCallback<MyWhirledData>() {
            public void onSuccess (MyWhirledData data) {
                init(data);
            }
        });
    }

    protected void init (MyWhirledData data)
    {
        FloatPanel buttonBar = new FloatPanel("ButtonBar");
        add(buttonBar);
        buttonBar.add(MsoyUI.createButton(MsoyUI.MEDIUM_THIN, "My Profile", Link.createListener(
            Pages.PEOPLE, CShell.getMemberId() + "")));
        buttonBar.add(MsoyUI.createButton(MsoyUI.MEDIUM_THIN, "Passport", Link.createListener(
            Pages.ME, "passport")));
        buttonBar.add(MsoyUI.createLabel(_msgs.populationDisplay("" + data.whirledPopulation),
            "PeopleOnline"));

        String empty = data.friendCount > 0 ? _pmsgs.emptyFeed() : _pmsgs.emptyFeedNoFriends();
        FriendsFeedPanel feed = new FriendsFeedPanel(empty, data.feed);
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
