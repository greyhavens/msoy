//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.landing.gwt.MeService;
import com.threerings.msoy.landing.gwt.MeServiceAsync;
import com.threerings.msoy.landing.gwt.MyWhirledData;
import com.threerings.msoy.person.gwt.FeedMessage;

import client.item.StuffNaviBar;
import client.msgs.MsgsMessages;
import client.people.FeedPanel;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class MyWhirled extends VerticalPanel
{
    public MyWhirled ()
    {
        setStyleName("myWhirled");

        _mesvc.getMyWhirled(CMe.ident, new MsoyCallback<MyWhirledData>() {
            public void onSuccess (MyWhirledData data) {
                init(data);
            }
        });
    }

    protected void init (MyWhirledData data)
    {
        add(new StuffNaviBar(Item.NOT_A_TYPE));
        add(MsoyUI.createLabel(CMe.msgs.populationDisplay("" + data.whirledPopulation), "Pop"));
        add(new WhatsNextPanel(data));
        String empty =
            data.friendCount > 0 ? _mmsgs.emptyFeed() : _mmsgs.emptyFeedNoFriends();
        FeedPanel feed = new FeedPanel(empty, true, new FeedPanel.FeedLoader() {
            public void loadFeed (int feedDays, AsyncCallback<List<FeedMessage>> callback) {
                _mesvc.loadFeed(CMe.ident, feedDays, callback);
            }
        });
        feed.setFeed(data.feed, false);
        add(feed);
    }

    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
