//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.orth.data.MediaDesc;

import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

/**
 * Generates the display for a feed message.
 */
public class FeedItemGenerator
{
    /**
     * Marker interface for a feed component which displays an image.
     */
    static public interface Media { }

    /**
     * Marker interface for a piece of the feed which displays an icon with some text associated.
     */
    static public interface Icon { }

    /**
     * Builds the feed as it is generated.
     */
    static public interface Builder
    {
        /**
         * Creates context-free media using the given descriptor that links to the provided page
         * and args.
         */
        Media createMedia (MediaDesc md, Pages page, Args args);

        /**
         * Creates context-free html that links to the given page + args and has the given label.
         */
        String createLink (String label, Pages page, Args args);

        /** Creates a context-free icon for when a user or users gains a level. */
        Icon createGainedLevelIcon (String html);

        /** Creates a context-free icon for club whirled subscriptions. */
        Icon createSubscribedIcon (String html);

        /** Creates a context-free icon for comment and forum reply notifications. */
        Icon createCommentedIcon (String html);

        /** Creates a context-free icon for poke notifications. */
        Icon createPokedIcon (String html);

        /** Creates a context-free icon for when a user founded a new group. */
        Icon createFoundedGroupIcon (String html);

        /** Creates a context-free icon for when a user listed a new item in the shop. */
        Icon createListedItemIcon (String html);

        /** Creates a context-free icon for when a user creates or modifies a room. */
        Icon createUpdatedRoomIcon (String html);

        /**
         * Adds a previously created media object to the feed with the given message. The passed
         * media will never be null.
         */
        void addMedia (Media media, String message);

        /**
         * Adds multiple previously created media objects to the feed with the given message. The
         * passed array will never be null nor empty.
         */
        void addMedia (Media media[], String message);

        /**
         * Adds a previously created icon to the feed.
         */
        void addIcon (Icon icon);

        /**
         * Adds plain text to the feed.
         */
        void addText (String text);
    }

    /**
     * Mode for action translations, indicates whether the subject, the object or neither is plural.
     */
    public enum Plural {NONE, SUBJECT, OBJECT}

    /**
     * Obtains translated strings for various parts of the feed as it is generated.
     */
    static public interface Messages
    {
        /** 2nd person singular. */
        String you ();

        /** Name of a given item type (numeric string). */
        String typeName (String itemType);

        /** Describes an item for inclusion in a new listing feed item. */
        String describeItem (String typeName, String itemName);

        /** Gets the name of a badge. */
        String badgeName (int code, String levelName);

        /** Gets the name of a medal. */
        String medal (String medal, String group);

        /** Gets the name of a person whose identity is not known. */
        String unknownMember ();

        /** Gets a string showing that someone gained a level, for embedding in a long list. */
        String briefLevelGain (String subject, String level);

        /**
         * Joins the subject and object with a verb based on the action, respecting pluralization
         * if appropriate.
         */
        String action (FeedMessage message, String subject, String object, Plural plural);

        /** Joins a list with the last element. */
        String andCombine (String list, String item);

        /** Joins a list with the next, non-last element. */
        String commaCombine (String list, String item);
    }

    /**
     * Creates a new feed generator.
     */
    public FeedItemGenerator (int memberId, boolean usePronouns, Builder display, Messages messages)
    {
        _memberId = memberId;
        _usePronouns = usePronouns;
        _builder = display;
        _messages = messages;
    }

    /**
     * Adds one message to the feed.
     */
    public void addMessage (FeedMessage message)
    {
        if (message instanceof FriendFeedMessage) {
            addFriendMessage((FriendFeedMessage)message);
        } else if (message instanceof GroupFeedMessage) {
            addGroupMessage((GroupFeedMessage)message);
        } else if (message instanceof SelfFeedMessage) {
            addSelfMessage((SelfFeedMessage)message);
        } else if (message instanceof AggregateFeedMessage) {
            AggregateFeedMessage aggMsg = (AggregateFeedMessage)message;
            switch (aggMsg.style) {
            case ACTIONS:
                addMultiActionsMessage(aggMsg.messages);
                break;

            case ACTORS:
                addMultiActorsMessage(aggMsg.messages);
                break;

            default:
                throw new IllegalArgumentException("Unknown aggregation style: " + aggMsg.style);
            }
        } else {
            addGlobalMessage(message);
        }
    }

    protected String action (FeedMessage message, String subject, String object, Plural plural)
    {
        // Capitalize the first letter, mostly to handle the capitalization of "You". This won't
        // touch user names since they're hyperlinked.
        return StringUtil.capitalize(_messages.action(message, subject, object, plural));
    }

    /**
     * Adds one friend message to the feed.
     */
    protected void addFriendMessage (FriendFeedMessage message)
    {
        String subject = buildSubject(message);
        String object = buildObject(message);
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
        case FRIEND_WON_TROPHY:
        case FRIEND_PLAYED_GAME:
        case FRIEND_WON_BADGE:
        case FRIEND_WON_MEDAL:
        case FRIEND_JOINED_GROUP:
            Media media = buildMedia(message);
            String text = action(message, subject, object, Plural.NONE);
            addMedia(media, text);
            break;

        case FRIEND_CREATED_GROUP:
            _builder.addIcon(_builder.createFoundedGroupIcon(
                action(message, subject, object, Plural.NONE)));
            break;

        case FRIEND_LISTED_ITEM:
            _builder.addIcon(_builder.createListedItemIcon(
                action(message, subject, object, Plural.NONE)));
            break;

        case FRIEND_UPDATED_ROOM:
            _builder.addIcon(_builder.createUpdatedRoomIcon(
                action(message, subject, object, Plural.NONE)));
            break;

        case FRIEND_GAINED_LEVEL:
            _builder.addIcon(_builder.createGainedLevelIcon(
                action(message, subject, object, Plural.NONE)));
            break;

        case FRIEND_SUBSCRIBED:
            _builder.addIcon(_builder.createSubscribedIcon(
                action(message, subject, object, Plural.NONE)));
            break;
        }
    }

    /**
     * Adds one group message to the feed.
     */
    protected void addGroupMessage (GroupFeedMessage message)
    {
        Media media = buildMedia(message);
        String object = buildObject(message);
        switch (message.type) {
        case GROUP_ANNOUNCEMENT:
            addMedia(media, action(
                message, message.data[0], object, Plural.NONE));
            break;

        case GROUP_UPDATED_ROOM:
            String groupLink = _builder.createLink(message.group.toString(), Pages.GROUPS,
                Args.compose("f", message.group.getGroupId()));
            addMedia(media, action(
                message, groupLink, object, Plural.NONE));
            break;

        default:
            break;
        }
    }

    /**
     * Adds one self message to the feed.
     */
    protected void addSelfMessage (SelfFeedMessage message)
    {
        String subject = buildSubject(message);
        String object = buildObject(message);
        String text = action(message, subject, object, Plural.NONE);
        switch (message.type) {
        case SELF_FORUM_REPLY:
        case SELF_ROOM_COMMENT:
        case SELF_ITEM_COMMENT:
        case SELF_GAME_COMMENT:
        case SELF_PROFILE_COMMENT:
            _builder.addIcon(_builder.createCommentedIcon(text));
            break;

        case SELF_POKE:
            _builder.addIcon(_builder.createPokedIcon(text));
            break;

        default:
            Media media = buildMedia(message);
            addMedia(media, text);
            break;
        }
    }

    /**
     * Adds several messages of the same type to the feed as one item. All items are expected to
     * have the same subject, e.g. "X did A, B and C".
     */
    protected void addMultiActionsMessage (List<FeedMessage> list)
    {
        FeedMessage message = list.get(0);
        String subject = buildSubject(message);
        switch (message.type) {
        case FRIEND_WON_BADGE:
        case FRIEND_WON_MEDAL:
        case FRIEND_ADDED_FRIEND:
        case FRIEND_WON_TROPHY:
        case FRIEND_PLAYED_GAME:
            Media[] media = buildMediaArray(list);
            String text = action(
                message, subject, makeStringList(list, ListMode.OBJECT), Plural.OBJECT);
            addMedia(media, text);
            break;

        case FRIEND_LISTED_ITEM:
            _builder.addIcon(_builder.createListedItemIcon(action(
                message, subject, makeStringList(list, ListMode.OBJECT), Plural.OBJECT)));
            break;

        case FRIEND_UPDATED_ROOM:
            _builder.addIcon(_builder.createUpdatedRoomIcon(action(
                message, subject, makeStringList(list, ListMode.OBJECT), Plural.OBJECT)));
            break;

        case SELF_FORUM_REPLY:
        case SELF_ROOM_COMMENT:
        case SELF_ITEM_COMMENT:
        case SELF_GAME_COMMENT:
        case SELF_PROFILE_COMMENT:
            _builder.addIcon(_builder.createCommentedIcon(action(
                message, subject, makeStringList(list, ListMode.OBJECT), Plural.OBJECT)));
            break;

        case SELF_POKE:
            _builder.addIcon(_builder.createPokedIcon(action(
                message, makeStringList(list, ListMode.OBJECT), "", Plural.OBJECT)));
            break;

        case FRIEND_GAINED_LEVEL:
            // display all levels gained by all friends together
            _builder.addIcon(_builder.createGainedLevelIcon(action(
                message, makeStringList(list, ListMode.LEVELGAIN), "", Plural.SUBJECT)));
            break;

        case FRIEND_SUBSCRIBED:
            _builder.addIcon(_builder.createSubscribedIcon(action(
                message, makeStringList(list, ListMode.LEVELGAIN), "", Plural.SUBJECT)));
            break;

        default:
            _builder.addText("Unknown actions aggregate type: " + message.type);
            break;
        }
    }

    /**
     * Adds several messages of the same type to the feed as one item. All items are expected to
     * have the same object, e.g. "X, Y and Z did A".
     */
    protected void addMultiActorsMessage (List<FeedMessage> list)
    {
        FeedMessage message = list.get(0);
        String friendLinks = makeStringList(list, ListMode.SUBJECT);
        String object = buildObject(message);
        String text = action(message, friendLinks, object, Plural.SUBJECT);
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
        case FRIEND_WON_TROPHY:
        case FRIEND_PLAYED_GAME:
        case FRIEND_WON_BADGE:
        case FRIEND_WON_MEDAL:
            Media media = buildMedia(message);
            addMedia(media, text);
            break;

        case SELF_ROOM_COMMENT:
        case SELF_ITEM_COMMENT:
        case SELF_GAME_COMMENT:
        case SELF_PROFILE_COMMENT:
        case SELF_FORUM_REPLY:
            _builder.addIcon(_builder.createCommentedIcon(text));
            break;

        case SELF_POKE:
            _builder.addIcon(_builder.createPokedIcon(text));
            break;

        default:
            _builder.addText("Unknown actors aggregate type: " + message.type);
            break;
        }
    }

    /**
     * Adds one global message to the feed.
     */
    protected void addGlobalMessage (FeedMessage message)
    {
        String object = buildObject(message);
        String text = action(message, "", object, Plural.NONE);
        switch (message.type) {
        case GLOBAL_ANNOUNCEMENT:
            _builder.addText(text);
            break;
        }
    }

    /**
     * Builds the object of the feed item, i.e. "Y" in "X did Y".
     */
    protected String buildObject (FeedMessage message)
    {
        switch (message.type.getCategory()) {
        case FRIENDINGS:
            return profileString(message.data[0], message.data[1]);

        case ROOMS:
            return _builder.createLink(
                message.data[1], Pages.WORLD, Args.compose("s" + message.data[0]));

        case GAMES:
            if (message.type == FeedMessageType.FRIEND_WON_TROPHY) {
                return _builder.createLink(message.data[0], Pages.GAMES,
                    SharedNaviUtil.GameDetails.TROPHIES.args(Integer.valueOf(message.data[1])));
            } else if (message.type == FeedMessageType.FRIEND_PLAYED_GAME) {
                return _builder.createLink(message.data[0], Pages.GAMES,
                    Args.compose("d", message.data[1]));
            }
            break;

        case LISTED_ITEMS:
            return _messages.describeItem(
                _messages.typeName(message.data[1]),
                    _builder.createLink(message.data[0], Pages.SHOP,
                        Args.compose("l", message.data[1], message.data[2])));

        case LEVELS:
            return (message.data.length > 0) ? message.data[0] : null;

        case BADGES:
            int badgeCode = Integer.parseInt(message.data[0]);
            int badgeLevel = Integer.parseInt(message.data[1]);
            String badgeName = _messages.badgeName(badgeCode, Badge.getLevelName(badgeLevel));
            int memberId = ((FriendFeedMessage)message).friend.getId();
            return _builder.createLink(badgeName, Pages.ME, Args.compose("passport", memberId));

        case MEDALS:
            memberId = ((FriendFeedMessage)message).friend.getId();
            String medalLink = _builder.createLink(
                message.data[0], Pages.ME, Args.compose("medals", memberId));
            String groupLink = _builder.createLink(
                message.data[2], Pages.GROUPS, Args.compose("d", message.data[3]));
            return _messages.medal(medalLink, groupLink);

        case GROUPS:
            return _builder.createLink(message.data[1], Pages.GROUPS,
                                       Args.compose("d", message.data[0]));

        case COMMENTS:
            if (message.type == FeedMessageType.SELF_ROOM_COMMENT) {
                return _builder.createLink(
                    message.data[1], Pages.ROOMS, Args.compose("room", message.data[0]));

            } else if (message.type == FeedMessageType.SELF_ITEM_COMMENT) {
                return _builder.createLink(message.data[2], Pages.SHOP,
                    Args.compose("l", message.data[0], message.data[1]));

            } else if (message.type == FeedMessageType.SELF_GAME_COMMENT) {
                return _builder.createLink(message.data[1], Pages.GAMES,
                    Args.compose("d", message.data[0]));

            } else if (message.type == FeedMessageType.SELF_PROFILE_COMMENT) {
                return _builder.createLink(message.data[1], Pages.PEOPLE,
                    Args.compose(message.data[0]));

            } else if (message.type == FeedMessageType.SELF_POKE) {
                return profileString(message.data[1], message.data[0]);
            }
            break;

        case FORUMS:
            return _builder.createLink(
                message.data[1], Pages.GROUPS, Args.compose("t", message.data[0]));

        case ANNOUNCEMENTS:
            switch (message.type) {
            case GROUP_ANNOUNCEMENT:
                return _builder.createLink(
                    message.data[1], Pages.GROUPS, Args.compose("t", message.data[2]));
            case GLOBAL_ANNOUNCEMENT:
                return _builder.createLink(
                    message.data[0], Pages.GROUPS, Args.compose("t", message.data[1]));
            }
        }

        return null;
    }

    /**
     * Builds the image media to display for a feed item.
     */
    protected Media buildMedia (final FeedMessage message)
    {
        MediaDesc media;
        switch (message.type.getCategory()) {
        case ROOMS:
            return buildMedia(message, 2, Pages.WORLD, "s" + message.data[0]);

        case GAMES:
            if (message.type == FeedMessageType.FRIEND_WON_TROPHY) {
                return buildMedia(message, 2, Pages.GAMES,
                    SharedNaviUtil.GameDetails.TROPHIES.args(Integer.valueOf(message.data[1])));
            }
            break;

        case BADGES:
            int badgeCode = Integer.parseInt(message.data[0]);
            int level = Integer.parseInt(message.data[1]);
            int memberId = ((FriendFeedMessage)message).friend.getId();
            media = EarnedBadge.getImageMedia(badgeCode, level);
            return _builder.createMedia(media, Pages.ME, Args.compose("passport", memberId));

        case MEDALS:
            int friendId = ((FriendFeedMessage)message).friend.getId();
            return buildMedia(message, 1, Pages.ME, "medals", friendId);

        case ANNOUNCEMENTS:
            return buildMedia(message, 3, Pages.GROUPS, "t", message.data[2]);
        }
        return null;
    }

    protected Media buildMedia (FeedMessage msg, int idx, Pages page, Object... args)
    {
        MediaDesc md = (idx >= 0 && idx < msg.data.length) ?
            CloudfrontMediaDesc.stringToCFMD(msg.data[idx]) : null;
        if (md == null) {
            return null;
        }
        return _builder.createMedia(md, page, Args.compose(args));
    }

    /**
     * Creates the media for a list of feed items by creating and concatenating the media for each
     * one.
     */
    protected Media[] buildMediaArray (List<FeedMessage> list)
    {
        List<Media> media = Lists.newArrayList();
        for (FeedMessage message : list) {
            Media w = buildMedia(message);
            if (w != null) {
                media.add(w);
            }
        }
        if (media.isEmpty()) {
            return null;
        }
        return media.toArray(new Media[media.size()]);
    }

    /**
     * Builds the subject of the feed item, i.e. "X" in "X did Y".
     */
    protected String buildSubject (FeedMessage message)
    {
        MemberName member;
        if (message instanceof FriendFeedMessage) {
            member = ((FriendFeedMessage)message).friend;
        } else if (message instanceof SelfFeedMessage) {
            member = ((SelfFeedMessage)message).actor;
        } else {
            member = null;
        }
        if (member == null) {
            // very old data may not include actor/friend
            return _messages.unknownMember();
        }
        return profileString(member.toString(), String.valueOf(member.getId()));
    }

    /**
     * Creates a link to a profile with the given name, using a pronoun if appropriate.
     */
    protected String profileString (String name, String id)
    {
        if (_usePronouns && id.trim().equals(_memberId + "")) {
            return _messages.you();
        }
        return _builder.createLink(name, Pages.PEOPLE, Args.compose(id));
    }

    /**
     * Combines the feed messages into a translated, comma separated list ending in 'and'. The mode
     * determines how each feed message is converted into a string.
     */
    protected String makeStringList (List<FeedMessage> list, ListMode mode)
    {
        String combine = mode.build(this, list.get(0));
        for (int ii = 1, ll = list.size(); ii < ll; ii++) {
            FeedMessage message = list.get(ii);
            if (ii + 1 == ll) {
                combine = _messages.andCombine(combine, mode.build(this, message));
            } else {
                combine = _messages.commaCombine(combine, mode.build(this, message));
            }
        }
        return combine;
    }

    /**
     * Wrapper for adding media to the build to make sure no null values are passed along.
     */
    protected void addMedia (Media media, String text)
    {
        if (media != null) {
            _builder.addMedia(media, text);
        } else {
            _builder.addText(text);
        }
    }

    /**
     * Wrapper for adding media to the build to make sure no null values are passed along.
     */
    protected void addMedia (Media[] media, String text)
    {
        if (media != null && media.length != 0) {
            _builder.addMedia(media, text);
        } else {
            _builder.addText(text);
        }
    }

    /**
     * Modes for converting a feed message to a string.
     */
    protected enum ListMode {
        /** Uses the subject provided by the generator. */
        SUBJECT {
            protected  String build (FeedItemGenerator generator, FeedMessage message) {
                return generator.buildSubject(message);
            }
        },
        /** Uses the object provided by the generator. */
        OBJECT {
            protected  String build (FeedItemGenerator generator, FeedMessage message) {
                return generator.buildObject(message);
            }
        },
        /** Uses the subject and object provided by the generator with a separator. */
        LEVELGAIN {
            protected String build (FeedItemGenerator generator, FeedMessage message) {
                return generator._messages.briefLevelGain(
                    SUBJECT.build(generator, message), OBJECT.build(generator, message));
            }
        };

        /** Converts a feed item to a string. */
        protected abstract String build (FeedItemGenerator generator, FeedMessage message);
    };

    /** The member viewing the feed. */
    protected int _memberId;

    /** Whether we use "you" instead of the member's name if it is the viewing member. */
    protected boolean _usePronouns;

    /** The builder we are generating to. */
    protected Builder _builder;

    /** The messages we use to create all our strings. */
    protected Messages _messages;
}
