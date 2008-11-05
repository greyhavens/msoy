//
//
// $Id: FeedPanel.java 12917 2008-10-28 20:10:30Z sarah $

package client.person;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.person.FeedMessageAggregator.AggregateFriendMessage;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.NaviUtil;

/**
 * Display a single news feed item, formatted based on type.
 */
public class FeedMessagePanel extends FlowPanel
{
    public FeedMessagePanel (FeedMessage message)
    {
        if (message instanceof FriendFeedMessage) {
            addFriendMessage((FriendFeedMessage)message);
        } else if (message instanceof GroupFeedMessage) {
            addGroupMessage((GroupFeedMessage)message);
        } else if (message instanceof SelfFeedMessage) {
            addSelfMessage((SelfFeedMessage)message);
        } else if (message instanceof AggregateFriendMessage) {
            if (((AggregateFriendMessage)message).left) {
                this.addLeftAggregateFriendMessage(((AggregateFriendMessage)message).messages);
            } else {
                this.addRightAggregateFriendMessage(((AggregateFriendMessage)message).messages);
            }
        } else {
            addMessage(message);
        }
    }

    protected void addFriendMessage (FriendFeedMessage message)
    {
        String friendLink = profileLink(message.friend);
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendAddedFriend(friendLink,
                buildString(message))));
            break;

        case 101: // FRIEND_UPDATED_ROOM
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendUpdatedRoom(friendLink,
                buildString(message))));
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
            String roomText = _pmsgs.selfRoomComment(profileLink(message.actor), roomPageLink,
                roomLink);
            add(new ThumbnailWidget(buildMedia(message), roomText));
            break;
        case 301: // SELF_ITEM_COMMENT
            String shopPageLink = Link.createHtml(message.data[2], Pages.SHOP, Args.compose("l",
                message.data[0], message.data[1]));
            String itemText = _pmsgs.selfItemComment(profileLink(message.actor), shopPageLink);
            add(new ThumbnailWidget(buildMedia(message), itemText));
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

    /**
     * Helper function which creates translated strings of a feed messages data.
     */
    protected String buildString (FeedMessage message)
    {
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
            return profileLink(message.data[0], message.data[1]);

        case 101: // FRIEND_UPDATED_ROOM
            return Link.createHtml(message.data[1], Pages.WORLD, "s" + message.data[0]);

        case 102: // FRIEND_WON_TROPHY
            return Link.createHtml(message.data[0], Pages.GAMES,
                                   NaviUtil.gameDetail(Integer.valueOf(message.data[1]),
                                                       NaviUtil.GameDetails.TROPHIES));

        case 103: // FRIEND_LISTED_ITEM
            return _pmsgs.descCombine(
                _dmsgs.xlate("itemType" + message.data[1]),
                        Link.createHtml(message.data[0], Pages.SHOP,
                            Args.compose("l", message.data[1], message.data[2])));

        case 104: // FRIEND_GAINED_LEVEL
            return message.data[0];

        case 105: // FRIEND_WON_BADGE
            int badgeCode = Integer.parseInt(message.data[0]);
            int badgeLevel = Integer.parseInt(message.data[1]);
            String badgeHexCode = Integer.toHexString(badgeCode);
            String badgeName =
                _dmsgs.get("badge_" + badgeHexCode, Badge.getLevelName(badgeLevel));

            int memberId = ((FriendFeedMessage)message).friend.getMemberId();
            return Link.createHtml(badgeName, Pages.ME, Args.compose("passport", memberId));
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
        case 100: // FRIEND_ADDED_FRIEND
            if (message.data.length < 3) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[2]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.PEOPLE, message.data[1]);
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

        case 101: // FRIEND_UPDATED_ROOM
            if (message.data.length < 3) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[2]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.WORLD, "s" + message.data[0]);
                }
            };
            // snapshots are unconstrained at a set size; fake a width constraint for TINY_SIZE.
            media.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
            return MediaUtil.createMediaView(media, MediaDesc.SNAPSHOT_TINY_SIZE, clicker);

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
            image.addClickListener(Link.createListener(Pages.ME, "passport"));
            return image;

        case 300: // SELF_ROOM_COMMENT
            if (message.data.length < 3) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[2]);
            if (media == null) {
                return null;
            }

            clicker = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.WORLD, Args.compose("s", message.data[0]));
                }
            };
            // snapshots are unconstrained at a set size; fake a width constraint for TINY_SIZE.
            media.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
            return MediaUtil.createMediaView(media, MediaDesc.SNAPSHOT_TINY_SIZE, clicker);

        case 301: // SELF_ITEM_COMMENT
            if (message.data.length < 4) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[3]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.SHOP, Args.compose("l", message.data[0], message.data[1]));
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);
        }
        return null;
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

    protected static class ThumbnailWidget extends SimplePanel
    {
        public ThumbnailWidget (Widget icon, String html)
        {
            this(icon == null ? null : new Widget[] { icon }, html);
        }

        public ThumbnailWidget (Widget[] icons, String html)
        {
            setStyleName("FeedWidget");
            InlinePanel contents = new InlinePanel("ThumbnailWidget");
            add(contents);
            if (icons != null && icons.length > 0) {
                for (Widget icon : icons) {
                    icon.addStyleName("ThumbnailContainer");
                    contents.add(icon);
                }
            }
            contents.add(MsoyUI.createHTML(html, "TextContainer"));
        }
    }

    public static class BasicWidget extends FlowPanel
    {
        public BasicWidget (String html)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedBasic");
            add(MsoyUI.createHTML(html, null));
        }
    }

    /**
     * Display multiple actions by the same person (eg listing new things in the shop).
     */
    protected void addLeftAggregateFriendMessage (List<FriendFeedMessage> list)
    {
        // friend feed messages are the only ones that get aggregated
        FriendFeedMessage message = list.get(0);
        String friendLink = profileLink(message.friend);
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendAddedFriends(friendLink,
                standardCombine(list))));
            break;

        case 101: // FRIEND_UPDATED_ROOM
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendUpdatedRooms(friendLink,
                standardCombine(list))));
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
            // display all levels gained by all friends together
            add(new IconWidget("friend_gained_level",
                        _pmsgs.friendsGainedLevel(friendLinkCombine(list))));
            break;

        case 105: // FRIEND_WON_BADGE
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendWonBadges(
                            friendLink, standardCombine(list))));
            break;

        default:
            add(new BasicWidget("Unknown left aggregate type: " + message.type));
            break;
        }
    }

    /**
     * Display multiple people performing the same action (eg winning the same trophy).
     */
    protected void addRightAggregateFriendMessage (List<FriendFeedMessage> list)
    {
        FriendFeedMessage message = list.get(0);
        String friendLinks = profileCombine(list);
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendAddedFriendsRight(
                friendLinks, buildString(message))));
            break;

        case 102: // FRIEND_WON_TROPHY
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonTrophy(
                            friendLinks, buildString(message))));
            break;

        case 105: // FRIEND_WON_BADGE
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonBadge(friendLinks,
                buildString(message))));
            break;

        default:
            add(new BasicWidget("Unknown right aggregate type: " + message.type));
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

    protected interface StringBuilder
    {
        String build (FriendFeedMessage message);
    }

    protected static final DateTimeFormat _dateFormater = DateTimeFormat.getFormat("MMMM d:");
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
