//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.threerings.msoy.web.gwt.DateUtil;

/**
 * Functions to aggregate a list of news feed messages by the actor (left aggregate) or the action
 * (right aggregate) in question.
 */
public class FeedMessageAggregator
{
    /**
     * Aggregate any messages with the same actor (left aggregate) or the same action (right
     * aggregate) and return a new message list containing aggregates and/or single messages to
     * display.
     * @param byDate If true, break up the aggregate messages by discrete days
     */
    public static List<FeedMessage> aggregate (FeedMessage[] oldMessages, boolean byDate)
    {
        if (oldMessages.length == 0) {
            return new ArrayList<FeedMessage>();
        }
        List<FeedMessage> newMessages = new ArrayList<FeedMessage>();

        List<FeedMessage> messages = new ArrayList<FeedMessage>();
        // messages needs to be mutable, and the list returned from Arrays.asList() is fixed
        // size, resulting in an annoying messages-less break in execution in GWT1.5 compiled
        // javascript
        messages.addAll(Arrays.asList(oldMessages));

        HashMap<MessageKey, MessageAggregate> actions = new HashMap<MessageKey, MessageAggregate>();
        HashMap<MessageKey, MessageAggregate> actors = new HashMap<MessageKey, MessageAggregate>();

        // if grouping by date, start with today then work backwards
        long header = byDate ? startOfDay(System.currentTimeMillis()) : 0;
        MessageAggregate dummy = new MessageAggregate();

        while (!messages.isEmpty()) {
            buildMessageMap(messages, header, actions, AggregateFeedMessage.Style.ACTIONS);
            buildMessageMap(messages, header, actors, AggregateFeedMessage.Style.ACTORS);
            FeedMessage message = null;

            for (Iterator<FeedMessage> msgIter = messages.iterator(); msgIter.hasNext();) {
                message = msgIter.next();
                if (header > message.posted) {
                    header = FeedMessageAggregator.startOfDay(message.posted);
                    break;
                }
                msgIter.remove();

                // Find the larger of the left or right aggregate message and display it
                MessageAggregate actionsValue = getDefault(actions, getActionsKey(message), dummy);
                MessageAggregate actorsValue = getDefault(actors, getActorsKey(message), dummy);
                int actionsSize = actionsValue.size();
                int actorsSize = actorsValue.size();

                // if one of the aggregate messages has been displayed, that means this message
                // is displayed and should be removed from any further aggregates
                // TODO: is this always true? what about the MAX_AGGREGATED_ITEMS limit?
                if (actionsValue.getDisplayed() || actorsValue.getDisplayed()) {
                    if (actionsValue.getDisplayed() && actorsSize > 1) {
                        actorsValue.remove(message);
                    } else if (actorsValue.getDisplayed() && actionsSize > 1) {
                        actionsValue.remove(message);
                    }
                    continue;
                }

                if (actionsSize >= actorsSize && actionsSize > 1) {
                    newMessages.add(new AggregateFeedMessage(AggregateFeedMessage.Style.ACTIONS,
                        message.type, message.posted, actionsValue.getList()));
                    if (actorsSize > 1) {
                        actorsValue.remove(message);
                    }
                    actionsValue.setDisplayed(true);
                    continue;
                } else if (actorsSize > 1) {
                    newMessages.add(new AggregateFeedMessage(AggregateFeedMessage.Style.ACTORS,
                        message.type, message.posted, actorsValue.getList()));
                    if (actionsSize > 1) {
                        actionsValue.remove(message);
                    }
                    actorsValue.setDisplayed(true);
                    continue;
                } else {
                    newMessages.add(message);
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
     * Gets the map entry if the key and value are not null, otherwise returns the dummy.
     */
    protected static MessageAggregate getDefault(
        HashMap<MessageKey, MessageAggregate> map, MessageKey key, MessageAggregate dummy)
    {
        MessageAggregate agg = key == null ? null : map.get(key);
        return agg == null ? dummy : agg;
    }

    /**
     * Get the key for left side aggregation. Multiple actions by the same person (eg listing new
     * things in the shop).
     */
    protected static MessageKey getActionsKey (FeedMessage message)
    {
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
        case FRIEND_UPDATED_ROOM:
        case FRIEND_WON_TROPHY:
        case FRIEND_LISTED_ITEM:
        case FRIEND_WON_BADGE:
        case FRIEND_WON_MEDAL:
            // group all these by the friend doing the actions
            return new MessageKey(message.type, ((FriendFeedMessage)message).friend.getMemberId());

        case FRIEND_GAINED_LEVEL:
            // all level gains by all friends are displayed together
            return new MessageKey(message.type, 0);
        }
        return null;
    }

    /**
     * Get the key for right side aggregation. Multiple people performing the same action (eg
     * winning the same trophy).
     */
    protected static MessageKey getActorsKey (FeedMessage message)
    {
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
            // one or more friends added a person to their friends, that person is the key
            return new MessageKey(message.type, message.data[1]);

        case FRIEND_WON_TROPHY:
            // one or more friends earned the same trophy; trophy name and id is the key
            return new MessageKey(message.type, message.data[1].concat(message.data[0]).hashCode());

        case FRIEND_WON_BADGE:
            // one or more friends earned the same badge; badge id and level is the key
            return new MessageKey(message.type, message.data[0].concat(message.data[1]).hashCode());

        case FRIEND_WON_MEDAL:
            // one or more friends earned the same medal; medal name and group id is the key
            return new MessageKey(message.type, message.data[0].concat(message.data[3]).hashCode());

        case SELF_ROOM_COMMENT:
            // one or more people commented on your room; scene id is the key
            return new MessageKey(message.type, message.data[0]);

        case SELF_ITEM_COMMENT:
            // one or more people commented on your shop item; catalog id is the key
            return new MessageKey(message.type, message.data[1]);
        }
        return null;
    }

    /**
     * Builds a left side or right side aggregated HashMap for the supplied messages.
     */
    protected static void buildMessageMap (List<FeedMessage> messages, long header,
        HashMap<MessageKey, MessageAggregate> map, AggregateFeedMessage.Style style)
    {
        map.clear();
        for (FeedMessage message : messages) {
            if (header > message.posted) {
                break;
            }
            MessageKey key = null;
            switch (style) {
            case ACTIONS: key = getActionsKey(message); break;
            case ACTORS: key = getActorsKey(message); break;
            }
            if (key == null) {
                continue;
            }
            MessageAggregate value = map.get(key);
            if (value == null) {
                value = new MessageAggregate();
                map.put(key, value);
            }

            if (value.size() < MAX_AGGREGATED_ITEMS) {
                value.add(message);
            }
        }
    }

    /**
     * A hashable key used for storing FeedMessages that will be aggregated.
     */
    protected static class MessageKey
    {
        public FeedMessageType type;
        public int key;

        public MessageKey (FeedMessageType type, String key)
        {
            this.type = type;
            try {
                this.key = Integer.valueOf(key);
            } catch (Exception e) {
                this.key = 0;
            }
        }

        public MessageKey (FeedMessageType type, int key)
        {
            this.type = type;
            this.key = key;
        }

        public int hashCode ()
        {
            return type.getCode() ^ key;
        }

        public boolean equals (Object o)
        {
            MessageKey other = (MessageKey)o;
            return type == other.type && key == other.key;
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
                // TODO: better way of logging in server/client shared code
                //CShell.log(
                //    "Ignoring addition of messages to a MessageAggregate that has been displayed");
                return;
            }
            list.add(message);
        }

        public void remove (FeedMessage message)
        {
            list.remove(message);
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

        public List<FeedMessage> getList ()
        {
            return list;
        }

        protected List<FeedMessage> list = new ArrayList<FeedMessage>();
    }

    /** Break aggregate messages up into maximum this many items each */
    protected static final int MAX_AGGREGATED_ITEMS = 5;
}
