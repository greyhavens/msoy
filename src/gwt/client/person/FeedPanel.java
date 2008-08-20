//
// $Id$

package client.person;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;

import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.DateUtil;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.NaviUtil;

public class FeedPanel extends TongueBox
{
    public static interface FeedLoader
    {
        void loadFeed (int feedDays, AsyncCallback<List<FeedMessage>> callback);
    }

    public FeedPanel (String emptyMessage, boolean setHeader, FeedLoader feedLoader)
    {
        if (setHeader) {
            setHeader(_pmsgs.headerFeed());
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

    /**
     * Get the key for left side aggregation.
     */
    protected static MessageKey getLeftKey (FeedMessage message)
    {
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
        case 102: // FRIEND_WON_TROPHY
        case 103: // FRIEND_LISTED_ITEM
        case 105: // FRIEND_WON_BADGE
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
        case 105: // FRIEND_WON_BADGE
            return new MessageKey(message.type, message.data[0].concat(message.data[1]).hashCode());
        }
        return null;
    }

    protected interface StringBuilder
    {
        String build (FriendFeedMessage message);
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
            messages = new ArrayList<FeedMessage>();
            // messages needs to be mutable, and the list returned from Arrays.asList() is fixed
            // size, resulting in an annoying messages-less break in execution in GWT1.5 compiled
            // javascript
            messages.addAll(Arrays.asList(messageArray));
            HashMap<MessageKey, MessageAggregate> messageMapLeft =
                new HashMap<MessageKey, MessageAggregate>();
            HashMap<MessageKey, MessageAggregate> messageMapRight =
                new HashMap<MessageKey, MessageAggregate>();

            long header = startofDay(System.currentTimeMillis());
            long yesterday = header - ONE_DAY;
            MessageAggregate dummyValue = new MessageAggregate();
            while (!messages.isEmpty()) {
                buildMessageMap(messages, header, messageMapLeft, true);
                buildMessageMap(messages, header, messageMapRight, false);

                FeedMessage message = null;
                for (Iterator<FeedMessage> msgIter = messages.iterator(); msgIter.hasNext(); ) {
                    message = msgIter.next();
                    if (header > message.posted) {
                        break;
                    }
                    msgIter.remove();
                    // Find the larger of the left or right aggregate message and display it
                    MessageKey lkey = getLeftKey(message);
                    MessageAggregate lvalue = lkey == null ? null : messageMapLeft.get(lkey);
                    lvalue = lvalue == null ? dummyValue : lvalue;
                    MessageKey rkey = getRightKey(message);
                    MessageAggregate rvalue = rkey == null ? null : messageMapRight.get(rkey);
                    rvalue = rvalue == null ? dummyValue : rvalue;
                    int lsize = lvalue.size();
                    int rsize = rvalue.size();
                    // if one of the aggregate messages has been displayed, that means this message
                    // is displayed and should be removed from any further aggregates
                    if (lvalue.getDisplayed() || rvalue.getDisplayed()) {
                        if (lvalue.getDisplayed() && rsize > 1) {
                            rvalue.remove(message);
                        } else if (rvalue.getDisplayed() && lsize > 1) {
                            lvalue.remove(message);
                        }
                        continue;
                    }
                    if (lsize >= rsize && lsize > 1) {
                        addLeftAggregateFriendMessage(lvalue.getList());
                        if (rsize > 1) {
                            rvalue.remove(message);
                        }
                        lvalue.setDisplayed(true);
                        continue;
                    } else if (rsize > 1) {
                        addRightAggregateFriendMessage(rvalue.getList());
                        if (lsize > 1) {
                            lvalue.remove(message);
                        }
                        rvalue.setDisplayed(true);
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
                messageMapLeft.clear();
                messageMapRight.clear();
            }
        }

        /**
         * Builds a left side or right side aggregated HashMap for the supplied messages.
         */
        protected void buildMessageMap (List<FeedMessage> messages, long header,
            HashMap<MessageKey, MessageAggregate> map, boolean left)
        {
            for (FeedMessage message : messages) {
                if (header > message.posted) {
                    break;
                }
                MessageKey key = (left ? getLeftKey(message) : getRightKey(message));
                if (key == null) {
                    continue;
                }
                MessageAggregate value = map.get(key);
                if (value == null) {
                    value = new MessageAggregate();
                    map.put(key, value);
                }
                value.add(message);
            }
        }

        protected long startofDay (long timestamp)
        {
            Date date = new Date(timestamp);
            DateUtil.zeroTime(date);
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
                return Link.createHtml(
                        message.data[1], Pages.WORLD, "s" + message.data[0]);

            case 102: // FRIEND_WON_TROPHY
                return Link.createHtml(message.data[0], Pages.GAMES,
                                       NaviUtil.gameDetail(Integer.valueOf(message.data[1]),
                                                           NaviUtil.GameDetails.TROPHIES));

            case 103: // FRIEND_LISTED_ITEM
                return _pmsgs.descCombine(
                            _dmsgs.getString("itemType" + message.data[1]),
                            Link.createHtml(message.data[0], Pages.SHOP,
                                Args.compose("l", message.data[1], message.data[2])));

            case 104: // FRIEND_GAINED_LEVEL
                return message.data[0];

            case 105: // FRIEND_WON_BADGE
                int badgeCode = Integer.parseInt(message.data[0]);
                int badgeLevel = Integer.parseInt(message.data[1]);
                String badgeHexCode = Integer.toHexString(badgeCode);
                String badgeName = _dmsgs.getString("badge_" + badgeHexCode) + " ("
                    + (badgeLevel + 1) + ")";
                return Link.createHtml(badgeName, Pages.ME, "passport");
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
                        Link.go(Pages.GAMES, NaviUtil.gameDetail(Integer.valueOf(message.data[1]),
                                                                NaviUtil.GameDetails.TROPHIES));
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
                        Link.go(
                            Pages.SHOP, Args.compose("l", message.data[1], message.data[2]));
                    }
                };
                return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

            case 105: // FRIEND_WON_BADGE
                int badgeCode = Integer.parseInt(message.data[0]);
                int level = Integer.parseInt(message.data[1]);
                Image image = new Image(EarnedBadge.getImageUrl(badgeCode, level));
                image.setWidth(MediaDesc.getWidth(MediaDesc.HALF_THUMBNAIL_SIZE) + "px");
                image.setHeight(MediaDesc.getHeight(MediaDesc.HALF_THUMBNAIL_SIZE) + "px");
                image.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        Link.go(Pages.ME, "passport");
                    }
                });
                return image;
            }

            return null;
        }

        protected void addFriendMessage (FriendFeedMessage message)
        {
            String friendLink = profileLink(message.friend);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new IconWidget("friend_added_friend", _pmsgs.friendAddedFriend(
                                friendLink, buildString(message))));
                break;

            case 101: // FRIEND_UPDATED_ROOM
                add(new IconWidget("friend_updated_room", _pmsgs.friendUpdatedRoom(
                                friendLink, buildString(message))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonTrophy(
                                friendLink, buildString(message))));
                break;

            case 103: // FRIEND_LISTED_ITEM
                add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendListedItem(
                                friendLink, buildString(message))));
                break;

            case 104: // FRIEND_GAINED_LEVEL
                add(new IconWidget("friend_gained_level", _pmsgs.friendGainedLevel(
                                friendLink, buildString(message))));
                break;

            case 105: // FRIEND_WON_BADGE
                add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonBadge(
                                friendLink, buildString(message))));
                break;
            }
        }

        protected String standardCombine (List<FriendFeedMessage> list)
        {
            return standardCombine(list, new StringBuilder() {
                public String build (FriendFeedMessage message) {
                    return buildString(message);
                }
            });
        }

        protected String friendLinkCombine (List<FriendFeedMessage> list)
        {
            return standardCombine(list, new StringBuilder() {
                public String build (FriendFeedMessage message) {
                    return _pmsgs.colonCombine(
                        profileLink(message.friend), buildString(message));
                }
            });
        }

        protected String profileCombine (List<FriendFeedMessage> list)
        {
            return standardCombine(list, new StringBuilder() {
                public String build (FriendFeedMessage message) {
                    return profileLink(message.friend);
                }
            });
        }

        /**
         * Helper function which combines the core feed message data into a translated, comma
         * separated and ending in 'and' list.
         */
        protected String standardCombine (List<FriendFeedMessage> list, StringBuilder builder)
        {
            String combine = builder.build(list.get(0));
            for (int ii = 1, ll = list.size(); ii < ll; ii++) {
                FriendFeedMessage message = list.get(ii);
                if (ii + 1 == ll) {
                    combine = _pmsgs.andCombine(combine, builder.build(message));
                } else {
                    combine = _pmsgs.commaCombine(combine, builder.build(message));
                }
            }
            return combine;
        }

        /**
         * Helper function which creates an array of media widgets from feed messages.
         */
        protected Widget[] buildMediaArray (List<FriendFeedMessage> list)
        {
            List<Widget> media = new ArrayList<Widget>();
            for (FriendFeedMessage message : list) {
                Widget w = buildMedia(message);
                if (w != null) {
                    media.add(w);
                }

            }
            if (media.isEmpty()) {
                return null;
            }
            return media.toArray(new Widget[media.size()]);
        }

        protected void addLeftAggregateFriendMessage (List<FriendFeedMessage> list)
        {
            // friend feed messages are the only ones that get aggregated
            FriendFeedMessage message = list.get(0);
            String friendLink = profileLink(message.friend);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new IconWidget("friend_added_friend", _pmsgs.friendAddedFriends(
                                friendLink, standardCombine(list))));
                break;

            case 101: // FRIEND_UPDATED_ROOM
                add(new IconWidget("friend_updated_room",
                            _pmsgs.friendsUpdatedRoom(friendLinkCombine(list))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendWonTrophies(
                                friendLink, standardCombine(list))));
                break;

            case 103: // FRIEND_LISTED_ITEM
                add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendListedItem(
                                friendLink, standardCombine(list))));
                break;

            case 104: // FRIEND_GAINED_LEVEL
                add(new IconWidget("friend_gained_level",
                            _pmsgs.friendsGainedLevel(friendLinkCombine(list))));
                break;

            case 105: // FRIEND_WON_BADGE
                add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendWonBadges(
                                friendLink, standardCombine(list))));
                break;
            }
        }

        protected void addRightAggregateFriendMessage (List<FriendFeedMessage> list)
        {
            FriendFeedMessage message = list.get(0);
            String friendLinks = profileCombine(list);
            switch (message.type) {
            case 100: // FRIEND_ADDED_FRIEND
                add(new IconWidget("friend_added_friend", _pmsgs.friendAddedFriendsRight(
                                friendLinks, buildString(message))));
                break;

            case 102: // FRIEND_WON_TROPHY
                add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonTrophy(
                                friendLinks, buildString(message))));
                break;

            case 105: // FRIEND_WON_BADGE
                add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonBadge(
                                friendLinks, buildString(message))));
                break;
            }
        }

        protected void addGroupMessage (GroupFeedMessage message)
        {
            switch (message.type) {
            case 200: // GROUP_ANNOUNCEMENT
                String threadLink = Link.createHtml(
                    message.data[1], Pages.WHIRLEDS, Args.compose("t", message.data[2]));
                add(new BasicWidget(_pmsgs.groupAnnouncement(message.data[0], threadLink)));
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
                String roomPageLink = Link.createHtml(_pmsgs.selfRoomCommented(),
                    Pages.WORLD, Args.compose("room", message.data[0]));
                String roomLink = Link.createHtml(
                    message.data[1], Pages.WORLD, "s" + message.data[0]);
                add(new BasicWidget(_pmsgs.selfRoomComment(
                                        profileLink(message.actor), roomPageLink, roomLink)));
                break;
            }
        }

        protected void addMessage (FeedMessage message)
        {
            switch (message.type) {
            case 1: // GLOBAL_ANNOUNCEMENT
                String threadLink = Link.createHtml(
                    message.data[0], Pages.WHIRLEDS, Args.compose("t", message.data[1]));
                add(new BasicWidget(_pmsgs.globalAnnouncement(threadLink)));
                break;
            }
        }

        protected String profileLink (MemberName friend)
        {
            return profileLink(friend.toString(), String.valueOf(friend.getMemberId()));
        }

        protected String profileLink (String name, String id)
        {
            return Link.createHtml(name, Pages.PEOPLE, id);
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
            add(MsoyUI.createHTML(html, null));
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

            setWidget(0, 1, MsoyUI.createHTML(html, null));
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

            setWidget(0, col, MsoyUI.createHTML(html, null));
            getFlexCellFormatter().addStyleName(0, col, "TextContainer");
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

    /**
     * A class to encapsulate the various purposes of the value in a message map.
     */
    protected static class MessageAggregate
    {
        public boolean displayed = false;

        public void add (FeedMessage message)
        {
            if (displayed) {
                CShell.log(
                    "Ignoring addition of messages to a MessageAggregate that has been displayed");
                return;
            }

            if (!(message instanceof FriendFeedMessage)) {
                // don't need to log it - this value is being stored with a throwaway key.
                return;
            }
            list.add((FriendFeedMessage)message);
        }

        public void remove (FeedMessage message)
        {
            if (message instanceof FriendFeedMessage) {
                list.remove(message);
            }
        }

        public int size ()
        {
            return list.size();
        }

        /**
         * Setting displayed to true clears out the map.
         */
        public void setDisplayed (boolean displayed)
        {
            this.displayed = displayed;
            if (displayed) {
                list.clear();
            }
        }

        public boolean getDisplayed ()
        {
            return displayed;
        }

        public List<FriendFeedMessage> getList ()
        {
            return list;
        }

        protected List<FriendFeedMessage> list = new ArrayList<FriendFeedMessage>();
    }

    protected FeedList _feeds;
    protected Label _moreLabel;
    protected String _emptyMessage;
    protected boolean _fullPage;
    protected FeedLoader _feedLoader;

    protected static final DateTimeFormat _dateFormater = DateTimeFormat.getFormat("MMMM d:");
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);

    /** The default number of days of feed information to show. */
    protected static final int SHORT_CUTOFF = 2;

    /** The default number of days of feed information to show. */
    protected static final int FULL_CUTOFF = 14;

    /** The length of one day in milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;
}
