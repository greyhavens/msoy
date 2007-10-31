//
// $Id$

package client.whirled;

import java.util.Arrays;
import java.util.Date;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.FriendFeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;

import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.util.MsoyUI;
import client.game.GameDetailPanel;

public class FeedPanel extends VerticalPanel
{
    public FeedPanel ()
    {
        buildUI();
    }

    public void setFeed (List feed, boolean fullPage)
    {
        _fullPage = fullPage;
        _feeds.clear();
        _feeds.populate(feed, _fullPage);
        _moreLabel.setText(_fullPage ? CWhirled.msgs.shortFeed() : CWhirled.msgs.fullFeed());
    }

    protected void loadFeed (final boolean fullPage)
    {
        int feedDays = fullPage ? FULL_CUTOFF : SHORT_CUTOFF;
        CWhirled.membersvc.loadFeed(CWhirled.ident, feedDays, new AsyncCallback() {
            public void onSuccess (Object result) {
                setFeed((List)result, fullPage);
            }
            public void onFailure(Throwable caught) {
                MsoyUI.error(CWhirled.serverError(caught));
            }
        });
    }

    protected void buildUI ()
    {
        setStyleName("FeedContainer");

        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        Label star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        Label title = new Label(CWhirled.msgs.headerFeed());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        add(header);

        add(_feeds = new FeedList());
        setHorizontalAlignment(ALIGN_RIGHT);
        add(_moreLabel = MsoyUI.createActionLabel("", "tipLabel", new ClickListener() {
            public void onClick (Widget sender) {
                loadFeed(!_fullPage);
            }
        }));
    }

    protected static class FeedList extends VerticalPanel
    {
        public FeedList ()
        {
            setStyleName("FeedList");
        }

        public void populate (List messages, boolean fullPage)
        {
            if (messages.size() == 0) {
                add(new BasicWidget(CWhirled.msgs.emptyFeed(
                            Application.createLinkToken(Page.WHIRLED, "whirledwide"))));
                return;
            }

            // sort in descending order by posted
            Object[] messageArray = messages.toArray();
            Arrays.sort(messageArray, new Comparator () {
                public int compare (Object o1, Object o2) {
                    if (!(o1 instanceof FeedMessage) || !(o2 instanceof FeedMessage)) {
                        return 0;
                    }
                    return (int)(((FeedMessage)o2).posted - ((FeedMessage)o1).posted);
                }
                public boolean equals (Object obj) {
                    return obj == this;
                }
            });
            messages = Arrays.asList(messageArray);

            long header = startofDay(System.currentTimeMillis());
            long yesterday = header - ONE_DAY;

            for (Iterator msgIter = messages.iterator(); msgIter.hasNext(); ) {
                FeedMessage message = (FeedMessage)msgIter.next();
                if (header > message.posted) {
                    header = startofDay(message.posted);
                    if (yesterday < message.posted) {
                        add(new DateWidget(CWhirled.msgs.yesterday()));
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

                if (message instanceof FriendFeedMessage) {
                    addFriendMessage((FriendFeedMessage)message);
                } else if (message instanceof GroupFeedMessage) {
                    addGroupMessage((GroupFeedMessage)message);
                } else {
                    addMessage(message);
                }
            }
        }

        protected long startofDay (long timestamp)
        {
            Date date = new Date(timestamp);
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            return date.getTime();
        }

        protected void addMessage (FeedMessage message)
        {
        }

        protected void addFriendMessage (FriendFeedMessage message)
        {
            String friendLink = profileLink(
                    message.friend.toString(), String.valueOf(message.friend.getMemberId()));
            switch (message.type) {
            // FRIEND_ADDED_FRIEND
            case 100:
                add(new BasicWidget(CWhirled.msgs.friendAddedFriend(
                            friendLink, profileLink(message.data[0], message.data[1]))));
                break;

            // FRIEND_UPDATED_ROOM
            case 101:
                add(new BasicWidget(CWhirled.msgs.friendUpdatedRoom(friendLink,
                                Application.createLinkToken(Page.WORLD, "s" + message.data[0]))));
                break;

            // FRIEND_WON_TROPHY
            case 102:
                add(new BasicWidget(CWhirled.msgs.friendWonTrophy(friendLink,
                                Application.createLinkHtml(message.data[0], Page.GAME,
                                    Args.compose(new String[] {
                                        "d", message.data[1], GameDetailPanel.TROPHIES_TAB })))));
                break;

            // FRIEND_LISTED_ITEM
            case 103:
                add(new BasicWidget(CWhirled.msgs.friendListedItem(friendLink,
                                CShell.dmsgs.getString("itemType" + message.data[1]),
                                Application.createLinkHtml(message.data[0], Page.CATALOG,
                                    Args.compose(new String[] {
                                        message.data[1], "i", message.data[2] })))));
                break;
            }

        }

        protected void addGroupMessage (GroupFeedMessage message)
        {
        }

        protected String profileLink (String name, String id)
        {
            return Application.createLinkHtml(name, Page.PROFILE, id);
        }
    }

    protected static class BasicWidget extends HorizontalPanel
    {
        public BasicWidget (String html)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedBasic");
            setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            add(new HTML(html));
        }
    }

    protected static class DateWidget extends HorizontalPanel
    {
        public DateWidget (Date date)
        {
            this(dateFormater.format(date));
        }

        public DateWidget (String label)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedDate");
            setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            add(new Label(label));
        }
    }

    protected static class FeedWidget extends HorizontalPanel
    {
        public FeedWidget (FeedMessage message)
        {
        }
    }

    protected FeedList _feeds;
    protected Label _moreLabel;

    protected boolean _fullPage;

    /** The default number of days of feed information to show. */
    protected static final int SHORT_CUTOFF = 2;

    /** The default number of days of feed information to show. */
    protected static final int FULL_CUTOFF = 14;

    /** The length of one day in milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    /** Date formater. */
    protected static final DateTimeFormat dateFormater = DateTimeFormat.getFormat("MMMM d:");
}
