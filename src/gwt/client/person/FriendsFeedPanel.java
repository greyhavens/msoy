//
// $Id: FeedPanel.java 12917 2008-10-28 20:10:30Z sarah $

package client.person;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;

/**
 * Display a News Feed of the activities your friends have been up to, for the Me page.
 */
public class FriendsFeedPanel extends FlowPanel
{
    public FriendsFeedPanel (String emptyMessage, List<FeedCategory> feed)
    {
        if (feed.size() == 0) {
            HorizontalPanel basicWidget = new HorizontalPanel();
            basicWidget.setStyleName("FeedWidget");
            basicWidget.addStyleName("FeedBasic");
            basicWidget.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            basicWidget.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            basicWidget.add(MsoyUI.createHTML(emptyMessage, null));
            add(basicWidget);
            return;
        }

        add(_feeds = MsoyUI.createFlowPanel("FeedList"));
        for (FeedCategory category : feed) {
            _feeds.add(MsoyUI.createLabel(_dmsgs.xlate("feedCategory" + category.type),
                "FeedCategoryHeader"));

            // combine feed items performed by the same person
            List<FeedMessage> messages = FeedMessageAggregator.aggregate(category.messages);

            for (FeedMessage message : messages) {
                _feeds.add(new FeedMessagePanel(message));
            }
        }
    }

    protected FlowPanel _feeds;
    protected String _emptyMessage;

    protected static final DateTimeFormat _dateFormater = DateTimeFormat.getFormat("MMMM d:");
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
