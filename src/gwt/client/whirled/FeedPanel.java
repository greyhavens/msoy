//
// $Id$

package client.whirled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;
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

import com.threerings.msoy.data.all.MemberName;
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
        CWhirled.worldsvc.loadFeed(CWhirled.ident, feedDays, new AsyncCallback() {
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

    /**
     * Get the key for left side aggregation.
     */
    protected static MessageKey getLeftKey (FeedMessage message)
    {
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
        case 102: // FRIEND_WON_TROPHY
        case 103: // FRIEND_LISTED_ITEM
            return new MessageKey(message.type, ((FriendFeedMessage)message).friend.getMemberId());

        case 101: // FRIEND_UPDATED_ROOM
        case 104: // FRIEND_GAINED_LEVEL
            return new MessageKey(message.type, 0);
        }
        return null;
    }

    /**
     * Get the key for right side aggregation.
     */
    protected static MessageKey getRightKey (FeedMessage message)
    {
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
            return new MessageKey(message.type, message.data[1]);
        case 102: // FRIEND_WON_TROPHY
            return new MessageKey(message.type, message.data[1].concat(message.data[0]).hashCode());
        }
        return null;
    }

    protected interface StringBuilder
    {
        public String build (FeedMessage message);
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
            HashMap messageMapLeft = new HashMap();
            HashMap messageMapRight = new HashMap();

            long header = startofDay(System.currentTimeMillis());
            long yesterday = header - ONE_DAY;
            while (!messages.isEmpty()) {
                buildMessageMap(messages, header, messageMapLeft, null);
                buildMessageMap(messages, header, messageMapRight, messageMapLeft);

                FeedMessage message = null;
                for (Iterator msgIter = messages.iterator(); msgIter.hasNext(); ) {
                    message = (FeedMessage)msgIter.next();
                    if (header > message.posted) {
                        break;
                    }
                    msgIter.remove();
                    MessageKey key = getLeftKey(message);
                    Object value = null;
                    if (key != null) {
                        value = messageMapLeft.get(key);
                        if (value instanceof ArrayList) {
                            addLeftAggregateFriendMessage((ArrayList)value);
                            continue;
                        }
                    }
                    key = getRightKey(message);
                    if (key != null) {
                        value = messageMapRight.get(key);
                        if (value instanceof ArrayList) {
                            addRightAggregateFriendMessage((ArrayList)value);
                            continue;
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
                messageMapLeft.clear();
                messageMapRight.clear();
            }
        }

        /**
         * Builds a left side aggregation map if only one HashMap is supplied, otherwise builds
         * a right side aggregation map.
         */
        protected void buildMessageMap (List messages, long header, HashMap map, HashMap lmap)
        {
            // build right side aggregation map
            for (Iterator msgIter = messages.iterator(); msgIter.hasNext(); ) {
                FeedMessage message = (FeedMessage)msgIter.next();
                if (header > message.posted) {
                    break;
                }
                MessageKey key = (lmap == null ? getLeftKey(message) : getRightKey(message));
                if (key == null) {
                    continue;
                }
                if (lmap != null) {
                    MessageKey lkey = getLeftKey(message);
                    if (lkey != null && lmap.get(lkey) instanceof ArrayList) {
                        continue;
                    }
                }
                Object value = map.get(key);
                if (value == null) {
                    map.put(key, message);
                    continue;
                }
                msgIter.remove();
                if (value instanceof FeedMessage) {
                    ArrayList list = new ArrayList();
                    list.add(value);
                    list.add(message);
                    map.put(key, list);
                } else {
                    ((ArrayList)value).add(message);
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

        /**
         * Helper function which creates translated strings of a feed messages data.
         */
        protected String buildString (FeedMessage message)
        {
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                return profileLink(message.data[0], message.data[1]);

            case 101: // FRIEND_UPDATED_ROOM
                // TEMP: remove after servers are 2 weeks past 11/06/2007
                if (message.data.length == 1) {
                    return Application.createLinkHtml(
                            CWhirled.msgs.room(((FriendFeedMessage)message).friend.toString()),
                            Page.WORLD, "s" + message.data[0]);
                }
                // ENDTEMP
                return Application.createLinkHtml(
                        message.data[1], Page.WORLD, "s" + message.data[0]);

            case 102: // FRIEND_WON_TROPHY
                return Application.createLinkHtml(message.data[0], Page.GAME,
                    Args.compose("d", message.data[1], GameDetailPanel.TROPHIES_TAB));

            case 103: // FRIEND_LISTED_ITEM
                return CWhirled.msgs.descCombine(
                            CShell.dmsgs.getString("itemType" + message.data[1]),
                            Application.createLinkHtml(message.data[0], Page.CATALOG,
                                Args.compose(message.data[1], "i", message.data[2])));

            case 104: // FRIEND_GAINED_LEVEL
                return message.data[0];
            }

            return null;
        }

        protected void addFriendMessage (FriendFeedMessage message)
        {
            String friendLink = profileLink(message.friend);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new BasicWidget(CWhirled.msgs.friendAddedFriend(
                                friendLink, buildString(message))));
                break;

            case 101: // FRIEND_UPDATED_ROOM
                add(new BasicWidget(CWhirled.msgs.friendUpdatedRoom(
                                friendLink, buildString(message))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new BasicWidget(CWhirled.msgs.friendWonTrophy(
                                friendLink, buildString(message))));
                break;

            case 103: // FRIEND_LISTED_ITEM
                add(new BasicWidget(CWhirled.msgs.friendListedItem(
                                friendLink, buildString(message))));
                break;

            case 104: // FRIEND_GAINED_LEVEL
                add(new BasicWidget(CWhirled.msgs.friendGainedLevel(
                                friendLink, buildString(message))));
                break;
            }
        }

        protected String standardCombine (ArrayList list)
        {

            return standardCombine(list, new StringBuilder() {
                public String build (FeedMessage message) {
                    return buildString(message);
                }
            });
        }

        protected String friendLinkCombine (ArrayList list)
        {
            return standardCombine(list, new StringBuilder() {
                public String build (FeedMessage message) {
                    return CWhirled.msgs.colonCombine(
                        profileLink(((FriendFeedMessage)message).friend), buildString(message));
                }
            });
        }

        protected String profileCombine (ArrayList list)
        {
            return standardCombine(list, new StringBuilder() {
                public String build (FeedMessage message) {
                    return profileLink(((FriendFeedMessage)message).friend);
                }
            });
        }

        /**
         * Helper function which combines the core feed message data into a translated, comma
         * separated and ending in 'and' list.
         */
        protected String standardCombine (ArrayList list, StringBuilder builder)
        {
            String combine = builder.build((FeedMessage)list.get(0));
            for (int ii = 1, ll = list.size(); ii < ll; ii++) {
                FeedMessage message = (FeedMessage)list.get(ii);
                if (ii + 1 == ll) {
                    combine = CWhirled.msgs.andCombine(combine, builder.build(message));
                } else {
                    combine = CWhirled.msgs.commaCombine(combine, builder.build(message));
                }
            }
            return combine;
        }

        protected void addLeftAggregateFriendMessage (ArrayList list)
        {
            FriendFeedMessage message = (FriendFeedMessage)((ArrayList)list).get(0);
            String friendLink = profileLink(message.friend);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new BasicWidget(CWhirled.msgs.friendAddedFriends(
                                friendLink, standardCombine(list))));
                break;

            case 101: // FRIEND_UPDATED_ROOM
                add(new BasicWidget(CWhirled.msgs.friendsUpdatedRoom(friendLinkCombine(list))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new BasicWidget(CWhirled.msgs.friendWonTrophies(
                                friendLink, standardCombine(list))));
                break;

            case 103: // FRIEND_LISTED_ITEM
                add(new BasicWidget(CWhirled.msgs.friendListedItem(
                                friendLink, standardCombine(list))));
                break;

            case 104: // FRIEND_GAINED_LEVEL
                add(new BasicWidget(CWhirled.msgs.friendsGainedLevel(friendLinkCombine(list))));
                break;
            }
        }

        protected void addRightAggregateFriendMessage (ArrayList list)
        {
            FriendFeedMessage message = (FriendFeedMessage)((ArrayList)list).get(0);
            String friendLinks = profileCombine(list);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new BasicWidget(CWhirled.msgs.friendAddedFriendsRight(
                                friendLinks, buildString(message))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new BasicWidget(CWhirled.msgs.friendWonTrophy(
                                friendLinks, buildString(message))));
                break;
            }
        }


        protected void addGroupMessage (GroupFeedMessage message)
        {
            switch (message.type) {
            case 200: // GROUP_ANNOUNCEMENT
                String threadLink = Application.createLinkHtml(
                    message.data[1], Page.GROUP, Args.compose("t", message.data[2]));
                add(new BasicWidget(CWhirled.msgs.groupAnnouncement(message.data[0], threadLink)));
                break;
            }
        }

        protected String profileLink (MemberName friend)
        {
            return profileLink(friend.toString(), String.valueOf(friend.getMemberId()));
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

    /**
     * A hashable key used for storing FeedMessages that will be aggregated.
     */
    protected static class MessageKey
    {
        public Integer type;
        public Integer key;

        public MessageKey (int type, String key)
        {
            this.type = new Integer(type);
            try {
                this.key = new Integer(key);
            } catch (Exception e) {
                this.key = new Integer(0);
            }
        }
        public MessageKey (int type, int key)
        {
            this.type = new Integer(type);
            this.key = new Integer(key);
        }

        public int hashCode ()
        {
            int code = type.hashCode();
            if (key != null) {
                code ^= key.hashCode();
            }
            return code;
        }

        public boolean equals (Object o)
        {
            MessageKey other = (MessageKey)o;
            return type.equals(other.type) && key.equals(other.key);
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
