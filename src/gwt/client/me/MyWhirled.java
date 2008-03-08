//
// $Id$

package client.me;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MyWhirledData;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.StuffNaviBar;
import client.util.TongueBox;

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

    protected static final int PEOPLE_COLUMNS = 6;
}
