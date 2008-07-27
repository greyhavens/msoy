//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.group.gwt.MyWhirledData;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.person.gwt.FeedMessage;

import client.msgs.FeedPanel;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.StuffNaviBar;

public class MyWhirled extends VerticalPanel
{
    public MyWhirled ()
    {
        setStyleName("myWhirled");

        CMe.worldsvc.getMyWhirled(CMe.ident, new MsoyCallback<MyWhirledData>() {
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
            data.friendCount > 0 ? CMe.mmsgs.emptyFeed() : CMe.mmsgs.emptyFeedNoFriends();
        FeedPanel feed = new FeedPanel(empty, true, new FeedPanel.FeedLoader() {
            public void loadFeed (int feedDays, AsyncCallback<List<FeedMessage>> callback) {
                CMe.worldsvc.loadFeed(CMe.ident, feedDays, callback);
            }
        });
        feed.setFeed(data.feed, false);
        add(feed);
    }
}
