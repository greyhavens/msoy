//
// $Id$

package client.me;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.MyWhirledData;

import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.StuffNaviBar;

public class MyWhirled extends VerticalPanel
{
    public MyWhirled ()
    {
        setStyleName("myWhirled");

        CMe.worldsvc.getMyWhirled(CMe.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((MyWhirledData)result);
            }
        });
    }

    protected void init (MyWhirledData data)
    {
        add(new StuffNaviBar(Item.NOT_A_TYPE));
        add(MsoyUI.createLabel(CMe.msgs.populationDisplay("" + data.whirledPopulation), "Pop"));
        add(new WhatsNextPanel(data));
        String empty = data.friendCount > 0 ? CMe.msgs.emptyFeed() : CMe.msgs.emptyFeedNoFriends();
        FeedPanel feed = new FeedPanel(empty);
        feed.setFeed(data.feed, false);
        add(feed);
    }
}
