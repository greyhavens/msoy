//
// $Id: FeedPanel.java 12917 2008-10-28 20:10:30Z sarah $

package client.person;

import java.util.List;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;

import client.person.FeedMessagePanel.BasicWidget;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Display a News Feed of the activities your friends have been up to, for the Me page.
 */
public class FriendsFeedPanel extends FlowPanel
{
    public FriendsFeedPanel (String emptyMessage, List<FeedCategory> feed)
    {
        addStyleName("FeedList");
        if (feed.size() == 0) {
            add(new BasicWidget(emptyMessage));
            return;
        }

        for (FeedCategory category : feed) {
            FlowPanel categoryPanel = new FlowPanel();
            fillCategory(categoryPanel, category, false);
            add(categoryPanel);
        }
    }

    /**
     * Create and return a widget containing the category header, messages and "show more" button.
     */
    protected void fillCategory (final FlowPanel categoryPanel, final FeedCategory category,
        final boolean fullSize)
    {
        categoryPanel.clear();
        if (category == null) {
            return;
        }

        String showMoreText = fullSize ? _pmsgs.shortFeed() : _pmsgs.fullFeed();
        categoryPanel.add(MsoyUI.createActionLabel(showMoreText, "FeedShowMore",
            new ClickListener() {
                public void onClick (Widget sender) {
                    _mesvc.loadFeedCategory(category.category, !fullSize,
                        new MsoyCallback<FeedCategory>() {
                            public void onSuccess (FeedCategory data) {
                                fillCategory(categoryPanel, data, !fullSize);
                            }
                        });
                }
            }));

        categoryPanel.add(MsoyUI.createLabel(_dmsgs.xlate("feedCategory" + category.category),
            "FeedCategoryHeader"));

        // combine feed items performed by the same person
        List<FeedMessage> messages = FeedMessageAggregator.aggregate(category.messages, false);

        for (FeedMessage message : messages) {
            categoryPanel.add(new FeedMessagePanel(message, true));
        }
    }

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
