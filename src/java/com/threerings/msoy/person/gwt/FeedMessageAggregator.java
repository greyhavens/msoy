//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.person.gwt.AggregateFeedMessage.Style;

/**
 * Functions to aggregate a list of news feed messages by the actor (left aggregate) or the action
 * (right aggregate) in question.
 */
public class FeedMessageAggregator
{
    /** Break aggregate messages up into maximum this many items each */
    public static final int MAX_AGGREGATED_ITEMS = 5;

    /**
     * Aggregates any messages with the same actor (left aggregate) or the same action (right
     * aggregate). Returns a new message list containing aggregates and/or single messages to
     * display.
     *
     * @param byDate if true, break up the aggregate messages by discrete days.
     */
    public static List<FeedMessage> aggregate (List<FeedMessage> messages, boolean byDate)
    {
        // first key all messages so we don't have to keep doing it
        List<KeyedMessage> keyedMessages = new ArrayList<KeyedMessage>(messages.size());
        for (FeedMessage message : messages) {
            keyedMessages.add(new KeyedMessage(message));
        }

        // resulting list with a mix of aggregated and non-aggregated messages
        List<FeedMessage> newMessages = Lists.newArrayList();

        // if grouping by date, start with today then work backwards
        long header = byDate ? startOfDay(System.currentTimeMillis()) : 0;

        while (!keyedMessages.isEmpty()) {
            // partition the messages in two ways
            Partition actions = new Partition(Style.ACTIONS, keyedMessages, header);
            Partition actors = new Partition(Style.ACTORS, keyedMessages, header);

            for (Iterator<KeyedMessage> msgIter = keyedMessages.iterator(); msgIter.hasNext();) {
                KeyedMessage message = msgIter.next();
                if (header > message.message.posted) {
                    header = FeedMessageAggregator.startOfDay(message.message.posted);
                    break;
                }
                msgIter.remove();

                // get the groups the message belongs to
                GroupCollection groups = new GroupCollection(message, actions, actors);

                Partition.Group destination = null;

                // if one of the groups has already been marked for display, put it in that one
                for (Partition.Group g : groups) {
                    if (g.isDisplayed()) {
                        destination = g;
                        break;
                    }
                }

                // otherwise find the larger of the two groups with at least 2 messages
                if (destination == null) {
                    for (Partition.Group g : groups) {
                        if (g.size() > 1 && g.size() >= groups.other(g).size()) {
                            destination = g;
                            break;
                        }
                    }
                }

                if (destination == null) {
                    // neither aggregate had more than one message, just display it singly
                    newMessages.add(message.message);

                } else {
                    // remove the message from the other group to prevent double display
                    groups.other(destination).remove(message);

                    // add to new messages if it is not yet displayed and not depleted
                    if (!destination.isDisplayed() && !destination.isDepleted()) {
                        newMessages.add(destination.display());
                    }
                }
            }
        }

        return newMessages;
    }

    /**
     * Calculate the miliseconds for the start of a given day.
     */
    public static long startOfDay (long timestamp)
    {
        Date date = new Date(timestamp);
        DateUtil.zeroTime(date);
        return date.getTime();
    }

    /**
     * Get the key for actions aggregation, i.e. multiple actions by the same person.
     */
    protected static MessageKey getActionsKey (FeedMessage message)
    {
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
        case FRIEND_UPDATED_ROOM:
        case FRIEND_WON_TROPHY:
        case FRIEND_PLAYED_GAME:
        case FRIEND_LISTED_ITEM:
        case FRIEND_WON_BADGE:
        case FRIEND_WON_MEDAL:
            // group all these by the friend doing the actions
            return MessageKey.explicit(message, ((FriendFeedMessage)message).friend.getId());

        case FRIEND_GAINED_LEVEL:
        case FRIEND_SUBSCRIBED:
            // all level gains by all friends are displayed together
            return MessageKey.explicit(message, 0);

        case FRIEND_LIKED_MUSIC:
            // All music likes are aggregated together
            return MessageKey.explicit(message, 0);

        case SELF_FORUM_REPLY:
            // group forum replies by the actor
            return MessageKey.explicit(message, ((SelfFeedMessage)message).actor.getId());
        }
        return null;
    }

    /**
     * Get the key for actor aggregation, i.e. multiple people performing the same action.
     */
    protected static MessageKey getActorsKey (FeedMessage message)
    {
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
            // one or more friends added a person to their friends, that person is the key
            return MessageKey.id(message, 1);

        case FRIEND_WON_TROPHY:
            // one or more friends earned the same trophy; trophy name and id is the key
            return MessageKey.dataHash(message, 1, 0);

        case FRIEND_PLAYED_GAME:
            // one or more friends played the same game; game id is the key
            return MessageKey.id(message, 1);

        case FRIEND_WON_BADGE:
            // one or more friends earned the same badge; badge id and level is the key
            return MessageKey.dataHash(message, 0, 1);

        case FRIEND_WON_MEDAL:
            // one or more friends earned the same medal; medal name and group id is the key
            return MessageKey.dataHash(message, 0, 3);

        case SELF_ROOM_COMMENT:
            // one or more people commented on your room; scene id is the key
            // Use the reply field too
            return MessageKey.dataHash(message, 0, 3);

        case SELF_ITEM_COMMENT:
            // one or more people commented on your shop item; catalog id is the key
            // Use the reply field too
            return MessageKey.dataHash(message, 1, 4);

        case SELF_GAME_COMMENT:
            // one or more people commented on your game; game id is the key
            // Use the reply field too
            return MessageKey.dataHash(message, 0, 3);

        case SELF_PROFILE_COMMENT:
            return MessageKey.dataHash(message, 0, 2);

        case SELF_POKE:
            return MessageKey.id(message, 1);

        case SELF_FORUM_REPLY:
            // one or more replies to a forum post; thread id is the key
            return MessageKey.id(message, 0);
        }
        return null;
    }

    /**
     * A hashable key used for storing FeedMessages that will be aggregated.
     */
    protected static class MessageKey
    {
        public static MessageKey id (FeedMessage msg, int dataIdx)
        {
            try {
                return new MessageKey(msg.type, Integer.valueOf(msg.data[dataIdx]));
            } catch (Exception e) {
                return new MessageKey(msg.type, 0);
            }
        }

        public static MessageKey dataHash (FeedMessage msg, int dataIdx1, int dataIdx2)
        {
            if (dataIdx2 < msg.data.length) {
                int key = msg.data[dataIdx1].concat(msg.data[dataIdx2]).hashCode();
                return new MessageKey(msg.type, key);
            } else {
                return id(msg, dataIdx1);
            }
        }

        public static MessageKey explicit (FeedMessage msg, int key)
        {
            return new MessageKey(msg.type, key);
        }

        public int hashCode ()
        {
            return _type.getCode() ^ _key;
        }

        public boolean equals (Object o)
        {
            MessageKey other = (MessageKey)o;
            return _type == other._type && _key == other._key;
        }

        private MessageKey (FeedMessageType type, int key)
        {
            this._type = type;
            this._key = key;
        }

        protected FeedMessageType _type;
        protected int _key;
    }

    /**
     * Tuple for holding a feed message with both its actors and actions keys pre-computed.
     */
    protected static class KeyedMessage
    {
        public FeedMessage message;
        public MessageKey actorsKey;
        public MessageKey actionsKey;

        public KeyedMessage (FeedMessage message) {
            this.message = message;
            this.actorsKey = getActorsKey(message);
            this.actionsKey = getActionsKey(message);
        }

        public MessageKey getKey (Style style) {
            switch (style) {
            case ACTIONS: return actionsKey;
            case ACTORS: return actorsKey;
            }
            return null; // not aggregated
        }

        public boolean equals (Object other) {
            return message.equals(((KeyedMessage)other).message);
        }

        public int hashCode () {
            return message.hashCode();
        }

        /**
         * Checks whether this message is a duplicate of another.
         */
        public boolean isDuplicateKey (KeyedMessage other, Style style) {
            MessageKey key1 = getKey(style);
            MessageKey key2 = other.getKey(style);
            if (key1 == null || key2 == null) {
                return false;
            }
            return key1.equals(key2);
        }
    }

    /**
     * Partitions a collection of messages into groups with a shared key.
     */
    protected static class Partition
    {
        /**
         * A group of messages that have the same key. The group may also be marked as displayed.
         */
        public class Group
        {
            /**
             * Removes a message from the group. Called when the message is already in another
             * larger group.
             */
            public void remove (KeyedMessage message)
            {
                _list.remove(message);
            }

            /**
             * Gets the current number of messages in this group.
             */
            public int size ()
            {
                return _size;
            }

            /**
             * Detects if this group has had all its messages removed. This can happen if all the
             * message got displayed in preceding groups of the opposite aggregation style and
             * hence removed from this one. This in turn can occur if a lot of players are doing
             * a small set of distinct actions, e.g. playing games.
             */
            public boolean isDepleted ()
            {
                return _list.size() == 0;
            }

            /**
             * Marks this group as being displayed and returns the aggregation of all messages. To
             * save memory, also clears the group since it is not needed any more. The group must
             * not be already displayed and not depleted or an exception is thrown.
             */
            public FeedMessage display ()
            {
                if (_displayed || _list.size() == 0) {
                    throw new RuntimeException();
                }

                _displayed = true;

                List<FeedMessage> messages = new ArrayList<FeedMessage>(
                    Math.min(_list.size(), MAX_AGGREGATED_ITEMS));
                for (KeyedMessage entry : _list) {
                    messages.add(entry.message);
                    if (messages.size() >= MAX_AGGREGATED_ITEMS) {
                        break;
                    }
                }
                FeedMessage first = messages.get(0);
                FeedMessage result = first;
                if (_list.size() > 1) {
                    result = new AggregateFeedMessage(_style, first.type, first.posted, messages);
                }
                _list.clear();
                return result;
            }

            /**
             * Checks if this group has been marked for display.
             */
            public boolean isDisplayed ()
            {
                return _displayed;
            }

            /**
             * Adds a new message to this group. Only needed during Grouper construction.
             */
            protected void add (KeyedMessage message)
            {
                if (_displayed) {
                    throw new RuntimeException();
                }

                _size++;

                // don't add messages that are duplicates with respect to their keys
                // (we know one key is equal, just check the other)
                Style style = _style.getOpposite();
                for (KeyedMessage existing : _list) {
                    if (existing.isDuplicateKey(message, style)) {
                        return;
                    }
                }
                _list.add(message);
            }

            protected boolean _displayed;
            protected int _size; // number of calls to add, not == _list.size()
            protected List<KeyedMessage> _list = Lists.newArrayList();
        }

        /**
         * Creates a new partition and divides up the given messages into groups where all the
         * messages in a group share a common key.
         */
        public Partition (Style style, List<KeyedMessage> messages, long header)
        {
            _style = style;
            build(messages, header);
        }

        /**
         * Gets the group that the given message belongs in. If no group exists, an empty group is
         * returned so that the caller need not check for null.
         */
        public Group get (KeyedMessage message)
        {
            MessageKey key = message.getKey(_style);
            Group agg = key == null ? null : _map.get(key);
            return agg == null ? _dummy : agg;
        }

        protected void build (List<KeyedMessage> messages, long header)
        {
            _map.clear();
            for (KeyedMessage message : messages) {
                if (header > message.message.posted) {
                    break;
                }
                MessageKey key = message.getKey(_style);
                if (key == null) {
                    continue;
                }
                Group group = _map.get(key);
                if (group == null) {
                    _map.put(key, group = new Group());
                }
                group.add(message);
            }
        }

        /** The style of this grouper. */
        protected Style _style;

        /**
         * Partition of messages with the same subject or object.
         */
        protected Map<MessageKey, Group> _map = Maps.newHashMap();

        /** Empty group. */
        protected Group _dummy = new Group();
    }

    /**
     * Provides a way to iterate over 2 groups and easily access the "other" one.
     */
    protected static class GroupCollection
        implements Iterable<Partition.Group>
    {
        /**
         * Initializes the collection to be the groups of the given message in each of 2 partitions.
         */
        public GroupCollection (KeyedMessage message, Partition actions, Partition actors)
        {
            _groups = new Partition.Group[] {
                actions.get(message), actors.get(message)};
        }

        /**
         * Gets the other group, that is the group that is not the given one.
         */
        public Partition.Group other (Partition.Group group)
        {
            return group == _groups[0] ? _groups[1] : _groups[0];
        }

        @Override // from Iterable
        public Iterator<Partition.Group> iterator ()
        {
            return new Iterator<Partition.Group>() {
                @Override public boolean hasNext () {
                    return _next < _groups.length;
                }

                @Override public Partition.Group next () {
                    return _groups[_next++];
                }

                @Override public void remove () {
                }

                protected int _next = 0;
            };
        }

        protected Partition.Group[] _groups;
    }
}
