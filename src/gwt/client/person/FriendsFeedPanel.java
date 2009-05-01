//
// $Id$

package client.person;

import java.util.List;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;

import client.person.FeedMessagePanel.BasicWidget;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.InfoCallback;
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

        String categoryTitle = _dmsgs.xlate("feedCategory" + category.category.ordinal());
        ClickHandler onClick = new ClickHandler() {
            public void onClick (ClickEvent event) {
                _mesvc.loadFeedCategory(
                    category.category, !fullSize, new InfoCallback<FeedCategory>() {
                    public void onSuccess (FeedCategory data) {
                        fillCategory(categoryPanel, data, !fullSize);
                    }
                });
            }
        };
        String showMoreText = fullSize ? _pmsgs.shortFeed() : _pmsgs.fullFeed();
        Label showMore = MsoyUI.createActionLabel(showMoreText, "FeedShowMore", onClick);
        categoryPanel.add(showMore);

        categoryPanel.add(MsoyUI.createLabel(categoryTitle, "FeedCategoryHeader"));

        // combine feed items performed by the same person
        List<FeedMessage> messages = FeedMessageAggregator.aggregate(category.messages, false);
        for (FeedMessage message : messages) {
            try {
                categoryPanel.add(new FeedMessagePanel(message, true));
            } catch (Exception e) {
                CShell.log("Failed to display feed message", "msg", message, e);
                add(MsoyUI.createLabel("Oops. Formatting error!", null));
            }
        }
    }

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
