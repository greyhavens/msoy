//
// $Id$

package client.person;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.person.gwt.FeedMessage;

import client.person.FeedMessagePanel.BasicWidget;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.MsoyCallback;

/**
 * Display a News Feed of activities a specific player has been up to, for their profile.
 */
public class PlayerFeedPanel extends FlowPanel
{
    public static interface FeedLoader
    {
        void loadFeed (int feedDays, AsyncCallback<List<FeedMessage>> callback);
    }

    public PlayerFeedPanel (String emptyMessage, FeedLoader feedLoader)
    {
        add(_feeds = new FeedList());
        _emptyMessage = emptyMessage;

        add(_moreLabel = MsoyUI.createActionLabel("", "FeedShowMore", new ClickListener() {
            public void onClick (Widget sender) {
                loadFeed(!_fullPage);
            }
        }));
        _feedLoader = feedLoader;
    }

    public void setFeed (List<FeedMessage> feed, boolean fullPage)
    {
        _fullPage = fullPage;
        _feeds.clear();
        _feeds.populate(feed, _emptyMessage, _fullPage);
        _moreLabel.setText(_fullPage ? _pmsgs.shortFeed() : _pmsgs.fullFeed());
    }

    protected void loadFeed (final boolean fullPage)
    {
        int feedDays = fullPage ? FULL_CUTOFF : SHORT_CUTOFF;
        _feedLoader.loadFeed(feedDays, new MsoyCallback<List<FeedMessage>>() {
            public void onSuccess (List<FeedMessage> messages) {
                setFeed(messages, fullPage);
            }
        });
    }

    protected static class FeedList extends FlowPanel
    {
        public FeedList ()
        {
            setStyleName("FeedList");
        }

        public void populate (List<FeedMessage> messages, String emptyMessage, boolean fullPage)
        {
            if (messages.size() == 0) {
                add(new BasicWidget(emptyMessage));
                return;
            }

            // sort messages in descending order by posted
            FeedMessage[] messageArray = messages.toArray(new FeedMessage[messages.size()]);
            Arrays.sort(messageArray, new Comparator<FeedMessage> () {
                public int compare (FeedMessage f1, FeedMessage f2) {
                    return f2.posted > f1.posted ? 1 : (f1.posted > f2.posted ? -1 : 0);
                }
                public boolean equals (Object obj) {
                    return obj == this;
                }
            });

            // combine multiple actions by the same member, or multiple members performing the
            // same action, grouping by date.
            messages = FeedMessageAggregator.aggregate(messageArray, true);

            long header = FeedMessageAggregator.startOfDay(System.currentTimeMillis());
            long yesterday = header - ONE_DAY;
            for (FeedMessage message : messages) {
                if (header > message.posted) {
                    header = FeedMessageAggregator.startOfDay(message.posted);
                    if (yesterday < message.posted) {
                        add(new DateWidget(_pmsgs.yesterday()));
                    } else if (!fullPage) {
                        // stop after displaying today and yesterday; we let the server send us 48
                        // hours of feed messages to account for timezone differences, but we
                        // actually only want to see things that happened today and yesterday in
                        // our timezone
                        break;
                    } else {
                        add(new DateWidget(new Date(header)));
                    }
                }

                add(new FeedMessagePanel(message, false));
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

    protected FeedList _feeds;
    protected Label _moreLabel;
    protected String _emptyMessage;
    protected boolean _fullPage;
    protected FeedLoader _feedLoader;

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
