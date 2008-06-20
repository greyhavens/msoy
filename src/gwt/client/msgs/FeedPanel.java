//
// $Id$

package client.msgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.FriendFeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;
import com.threerings.msoy.person.data.SelfFeedMessage;

import client.games.GameDetailPanel;
import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.TongueBox;

public class FeedPanel extends TongueBox
{
    public static interface FeedLoader
    {
        public void loadFeed (int feedDays, AsyncCallback callback);
    }

    public FeedPanel (String emptyMessage, boolean setHeader, FeedLoader feedLoader)
    {
        if (setHeader) {
            setHeader(CMsgs.mmsgs.headerFeed());
        }
        setContent(_feeds = new FeedList());
        _emptyMessage = emptyMessage;
        _moreLabel = setFooterLabel("", new ClickListener() {
            public void onClick (Widget sender) {
                loadFeed(!_fullPage);
            }
        });
        _feedLoader = feedLoader;
    }

    public void setFeed (List<FeedMessage> feed, boolean fullPage)
    {
        _fullPage = fullPage;
        _feeds.clear();
        _feeds.populate(feed, _emptyMessage, _fullPage);
        _moreLabel.setText(_fullPage ? CMsgs.mmsgs.shortFeed() : CMsgs.mmsgs.fullFeed());
    }

    protected void loadFeed (final boolean fullPage)
    {
        int feedDays = fullPage ? FULL_CUTOFF : SHORT_CUTOFF;
        _feedLoader.loadFeed(feedDays, new AsyncCallback() {
            public void onSuccess (Object result) {
                setFeed((List)result, fullPage);
            }
            public void onFailure(Throwable caught) {
                MsoyUI.error(CMsgs.serverError(caught));
            }
        });
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

        public void populate (List<FeedMessage> messages, String emptyMessage, boolean fullPage)
        {
            if (messages.size() == 0) {
                add(new BasicWidget(emptyMessage));
                return;
            }

            // sort in descending order by posted
            FeedMessage[] messageArray = messages.toArray(new FeedMessage[messages.size()]);
            Arrays.sort(messageArray, new Comparator<FeedMessage> () {
                public int compare (FeedMessage f1, FeedMessage f2) {
                    return f2.posted > f1.posted ? 1 : (f1.posted > f2.posted ? -1 : 0);
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
                buildMessageMap(messages, header, messageMapLeft, true);
                buildMessageMap(messages, header, messageMapRight, false);

                FeedMessage message = null;
                for (Iterator msgIter = messages.iterator(); msgIter.hasNext(); ) {
                    message = (FeedMessage)msgIter.next();
                    if (header > message.posted) {
                        break;
                    }
                    msgIter.remove();
                    // Find the larger of the left or right aggregate message and display it
                    MessageKey lkey = getLeftKey(message);
                    Object lvalue = null;
                    if (lkey != null) {
                        lvalue = messageMapLeft.get(lkey);
                    }
                    MessageKey rkey = getRightKey(message);
                    Object rvalue = null;
                    if (rkey != null) {
                        rvalue = messageMapRight.get(rkey);
                    }
                    int lsize = (lvalue == null ? 0 :
                            (lvalue instanceof ArrayList ? ((ArrayList)lvalue).size() : 1));
                    int rsize = (rvalue == null ? 0 :
                            (rvalue instanceof ArrayList ? ((ArrayList)rvalue).size() : 1));
                    // if one of the aggregate messages has been displayed, that means this message
                    // is displayed and should be removed from any further aggregates
                    if (lvalue instanceof Boolean || rvalue instanceof Boolean) {
                        if (lvalue instanceof Boolean && rsize > 1) {
                            ((ArrayList)rvalue).remove(message);
                        } else if (rvalue instanceof Boolean && lsize > 1) {
                            ((ArrayList)lvalue).remove(message);
                        }
                        continue;
                    }
                    if (lsize >= rsize && lsize > 1) {
                        addLeftAggregateFriendMessage((ArrayList)lvalue);
                        if (rsize > 1) {
                            ((ArrayList)rvalue).remove(message);
                        }
                        messageMapLeft.put(lkey, new Boolean(true));
                        continue;
                    } else if (rsize > 1) {
                        addRightAggregateFriendMessage((ArrayList)rvalue);
                        if (lsize > 1) {
                            ((ArrayList)lvalue).remove(message);
                        }
                        messageMapRight.put(rkey, new Boolean(true));
                        continue;
                    }
                    // if this message is not aggregated than display it individually
                    if (message instanceof FriendFeedMessage) {
                        addFriendMessage((FriendFeedMessage)message);
                    } else if (message instanceof GroupFeedMessage) {
                        addGroupMessage((GroupFeedMessage)message);
                    } else if (message instanceof SelfFeedMessage) {
                        addSelfMessage((SelfFeedMessage)message);
                    } else {
                        addMessage(message);
                    }
                }
                if (header > message.posted) {
                    header = startofDay(message.posted);
                    if (yesterday < message.posted) {
                        add(new DateWidget(CMsgs.mmsgs.yesterday()));
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
         * Builds a left side or right side aggregated HashMap for the supplied messages.
         */
        protected void buildMessageMap (List<FeedMessage> messages, long header, HashMap map, 
            boolean left)
        {
            for (FeedMessage message : messages) {
                if (header > message.posted) {
                    break;
                }
                MessageKey key = (left ? getLeftKey(message) : getRightKey(message));
                if (key == null) {
                    continue;
                }
                Object value = map.get(key);
                if (value == null) {
                    map.put(key, message);
                    continue;
                }
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

        /**
         * Helper function which creates translated strings of a feed messages data.
         */
        protected String buildString (FeedMessage message)
        {
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                return profileLink(message.data[0], message.data[1]);

            case 101: // FRIEND_UPDATED_ROOM
                return Application.createLinkHtml(
                        message.data[1], Page.WORLD, "s" + message.data[0]);

            case 102: // FRIEND_WON_TROPHY
                return Application.createLinkHtml(message.data[0], Page.GAMES,
                    Args.compose("d", message.data[1], GameDetailPanel.TROPHIES_TAB));

            case 103: // FRIEND_LISTED_ITEM
                return CMsgs.mmsgs.descCombine(
                            CShell.dmsgs.getString("itemType" + message.data[1]),
                            Application.createLinkHtml(message.data[0], Page.SHOP,
                                Args.compose("l", message.data[1], message.data[2])));

            case 104: // FRIEND_GAINED_LEVEL
                return message.data[0];
            }

            return null;
        }

        /**
         * Helper function which creates a clickable widget from the supplied media information.
         */
        protected Widget buildMedia (final FeedMessage message)
        {
            MediaDesc media;
            ClickListener clicker;
            switch (message.type) {
            case 102: // FRIEND_WON_TROPHY
                media = MediaDesc.stringToMD(message.data[2]);
                if (media == null) {
                    return null;
                }
                clicker = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.GAMES, Args.compose("d", message.data[1],
                                                                GameDetailPanel.TROPHIES_TAB));
                    }
                };
                return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

            case 103: // FRIEND_LISTED_ITEM
                if (message.data.length < 4) {
                    return null;
                }
                media = MediaDesc.stringToMD(message.data[3]);
                if (media == null) {
                    return null;
                }
                clicker = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(
                            Page.SHOP, Args.compose("l", message.data[1], message.data[2]));
                    }
                };
                return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);
            }
            return null;
        }

        protected void addFriendMessage (FriendFeedMessage message)
        {
            String friendLink = profileLink(message.friend);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new IconWidget("friend_added_friend", CMsgs.mmsgs.friendAddedFriend(
                                friendLink, buildString(message))));
                break;

            case 101: // FRIEND_UPDATED_ROOM
                add(new IconWidget("friend_updated_room", CMsgs.mmsgs.friendUpdatedRoom(
                                friendLink, buildString(message))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new ThumbnailWidget(buildMedia(message), CMsgs.mmsgs.friendWonTrophy(
                                friendLink, buildString(message))));
                break;

            case 103: // FRIEND_LISTED_ITEM
                add(new ThumbnailWidget(buildMedia(message), CMsgs.mmsgs.friendListedItem(
                                friendLink, buildString(message))));
                break;

            case 104: // FRIEND_GAINED_LEVEL
                add(new IconWidget("friend_gained_level", CMsgs.mmsgs.friendGainedLevel(
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
                    return CMsgs.mmsgs.colonCombine(
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
                    combine = CMsgs.mmsgs.andCombine(combine, builder.build(message));
                } else {
                    combine = CMsgs.mmsgs.commaCombine(combine, builder.build(message));
                }
            }
            return combine;
        }

        /**
         * Helpfer function with creates an array of media widgets from feed messages.
         */
        protected Widget[] buildMediaArray (ArrayList list)
        {
            ArrayList media = new ArrayList();
            for (int ii = 0, ll = list.size(); ii < ll; ii++) {
                Widget w = buildMedia((FeedMessage)list.get(ii));
                if (w != null) {
                    media.add(w);
                }
            }
            if (media.isEmpty()) {
                return null;
            }
            return (Widget[])media.toArray(new Widget[media.size()]);
        }

        protected void addLeftAggregateFriendMessage (ArrayList list)
        {
            FriendFeedMessage message = (FriendFeedMessage)((ArrayList)list).get(0);
            String friendLink = profileLink(message.friend);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new IconWidget("friend_added_friend", CMsgs.mmsgs.friendAddedFriends(
                                friendLink, standardCombine(list))));
                break;

            case 101: // FRIEND_UPDATED_ROOM
                add(new IconWidget("friend_updated_room",
                            CMsgs.mmsgs.friendsUpdatedRoom(friendLinkCombine(list))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new ThumbnailWidget(buildMediaArray(list), CMsgs.mmsgs.friendWonTrophies(
                                friendLink, standardCombine(list))));
                break;

            case 103: // FRIEND_LISTED_ITEM
                add(new ThumbnailWidget(buildMediaArray(list), CMsgs.mmsgs.friendListedItem(
                                friendLink, standardCombine(list))));
                break;

            case 104: // FRIEND_GAINED_LEVEL
                add(new IconWidget("friend_gained_level",
                            CMsgs.mmsgs.friendsGainedLevel(friendLinkCombine(list))));
                break;
            }
        }

        protected void addRightAggregateFriendMessage (ArrayList list)
        {
            FriendFeedMessage message = (FriendFeedMessage)((ArrayList)list).get(0);
            String friendLinks = profileCombine(list);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new IconWidget("friend_added_friend", CMsgs.mmsgs.friendAddedFriendsRight(
                                friendLinks, buildString(message))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new ThumbnailWidget(buildMedia(message), CMsgs.mmsgs.friendWonTrophy(
                                friendLinks, buildString(message))));
                break;
            }
        }

        protected void addGroupMessage (GroupFeedMessage message)
        {
            switch (message.type) {
            case 200: // GROUP_ANNOUNCEMENT
                String threadLink = Application.createLinkHtml(
                    message.data[1], Page.WHIRLEDS, Args.compose("t", message.data[2]));
                add(new BasicWidget(CMsgs.mmsgs.groupAnnouncement(message.data[0], threadLink)));
                break;
            }
        }

        protected void addSelfMessage (SelfFeedMessage message)
        {
            switch (message.type) {
            case 300: // SELF_ROOM_COMMENT
                if (message.actor == null) {
                    return; // TEMP: skip old pre-actor messages
                }
                String roomPageLink = Application.createLinkHtml(CMsgs.mmsgs.selfRoomCommented(),
                    Page.WORLD, Args.compose("room", message.data[0]));
                String roomLink = Application.createLinkHtml(
                    message.data[1], Page.WORLD, "s" + message.data[0]);
                add(new BasicWidget(CMsgs.mmsgs.selfRoomComment(
                                        profileLink(message.actor), roomPageLink, roomLink)));
                break;
            }
        }

        protected void addMessage (FeedMessage message)
        {
            switch (message.type) {
            case 1: // GLOBAL_ANNOUNCEMENT
                String threadLink = Application.createLinkHtml(
                    message.data[0], Page.WHIRLEDS, Args.compose("t", message.data[1]));
                add(new BasicWidget(CMsgs.mmsgs.globalAnnouncement(threadLink)));
                break;
            }
        }

        protected String profileLink (MemberName friend)
        {
            return profileLink(friend.toString(), String.valueOf(friend.getMemberId()));
        }

        protected String profileLink (String name, String id)
        {
            return Application.createLinkHtml(name, Page.PEOPLE, id);
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

    protected static class IconWidget extends FlexTable
    {
        public IconWidget (String icon, String html)
        {
            setStyleName("FeedWidget");
            setCellSpacing(0);
            setCellPadding(0);

            Image image = new Image("/images/whirled/" + icon + ".png");
            image.setStyleName("FeedIcon");
            setWidget(0, 0, image);
            getFlexCellFormatter().setStyleName(0, 0, "IconContainer");

            setWidget(0, 1, new HTML(html));
            getFlexCellFormatter().addStyleName(0, 1, "TextContainer");
        }
    }

    protected static class ThumbnailWidget extends FlexTable
    {
        public ThumbnailWidget (Widget icon, String html)
        {
            this(icon == null ? null : new Widget[] { icon }, html);
        }

        public ThumbnailWidget (Widget[] icons, String html)
        {
            setStyleName("FeedWidget");
            setCellSpacing(0);
            setCellPadding(0);
            int col = 0;

            if (icons != null && icons.length > 0) {
                FlexTable iconContainer = new FlexTable();
                iconContainer.setCellSpacing(2);
                iconContainer.setCellPadding(0);
                for (int ii = 0; ii < icons.length; ii++) {
                    iconContainer.setWidget(0, ii, icons[ii]);
                    iconContainer.getFlexCellFormatter().setStyleName(0, ii, "ThumbnailContainer");
                }
                setWidget(0, col, iconContainer);
                getFlexCellFormatter().setStyleName(0, col++, "ThumbnailContainer");
            }

            setWidget(0, col, new HTML(html));
            getFlexCellFormatter().addStyleName(0, col, "TextContainer");
        }
    }

    protected static class DateWidget extends Label
    {
        public DateWidget (Date date)
        {
            this(dateFormater.format(date));
        }
        public DateWidget (String label)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedDate");
            setText(label);
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
    protected String _emptyMessage;
    protected boolean _fullPage;
    protected FeedLoader _feedLoader;

    /** The default number of days of feed information to show. */
    protected static final int SHORT_CUTOFF = 2;

    /** The default number of days of feed information to show. */
    protected static final int FULL_CUTOFF = 14;

    /** The length of one day in milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    /** Date formater. */
    protected static final DateTimeFormat dateFormater = DateTimeFormat.getFormat("MMMM d:");
}
