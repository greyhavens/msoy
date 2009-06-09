//
// $Id$

package client.person;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;

/**
 * Display a News Feed of activities a specific player has been up to, for their profile.
 */
public class PlayerFeedPanel extends FlowPanel
{
    public PlayerFeedPanel (String emptyMessage, List<FeedMessage> feed)
    {
        if (feed.size() == 0) {
            add(MsoyUI.createLabel(emptyMessage, null));
        } else {
            add(new FeedList(feed));
        }
    }

    protected static class FeedList extends FlowPanel
    {
        public FeedList (List<FeedMessage> messages)
        {
            setStyleName("FeedList");

            // combine multiple actions by the same member, or multiple members performing the same
            // action, grouping by date
            messages = FeedMessageAggregator.aggregate(messages, true);

            long header = FeedMessageAggregator.startOfDay(System.currentTimeMillis());
            long yesterday = header - ONE_DAY;
            for (FeedMessage message : messages) {
                if (header > message.posted) {
                    header = FeedMessageAggregator.startOfDay(message.posted);
                    if (yesterday < message.posted) {
                        add(new DateWidget(_pmsgs.yesterday()));
                    } else {
                        add(new DateWidget(new Date(header)));
                    }
                }

                try {
                    add(new FeedMessagePanel(message, false));
                } catch (Exception e) {
                    CShell.log("Failed to display feed message", "msg", message, e);
                    add(MsoyUI.createLabel("Oops. Formatting error!", null));
                }
            }
        }
    }

    protected static class DateWidget extends Label
    {
        public DateWidget (Date date)
        {
            this(_dateFormater.format(date));
        }
        public DateWidget (String label)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedDate");
            setText(label);
        }
    }

    protected static final DateTimeFormat _dateFormater = DateTimeFormat.getFormat("MMMM d:");
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);

    /** The default number of days of feed information to show. */
    public static final int SHORT_CUTOFF = 2;

    /** The default number of days of feed information to show. */
    public static final int FULL_CUTOFF = 14;

    /** The length of one day in milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;
}
