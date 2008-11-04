//
// $Id: FeedPanel.java 12917 2008-10-28 20:10:30Z sarah $

package client.person;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FriendFeedMessage;

import client.shell.CShell;
import client.util.DateUtil;

/**
 * Functions to aggregate a list of news feed messages by the actor (left aggregate) or the action
 * (right aggregate) in question.
 */
public class FeedMessageAggregator extends FlowPanel
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

        HashMap<MessageKey, MessageAggregate> messageMapLeft =
            new HashMap<MessageKey, MessageAggregate>();
        HashMap<MessageKey, MessageAggregate> messageMapRight =
            new HashMap<MessageKey, MessageAggregate>();

        // if grouping by date, start with today then work backwards
        long header = byDate ? startOfDay(System.currentTimeMillis()) : 0;
        MessageAggregate dummyValue = new MessageAggregate();

        while (!messages.isEmpty()) {
            buildMessageMap(messages, header, messageMapLeft, true);
            buildMessageMap(messages, header, messageMapRight, false);
            FeedMessage message = null;

            for (Iterator<FeedMessage> msgIter = messages.iterator(); msgIter.hasNext();) {
                message = msgIter.next();
                if (header > message.posted) {
                    header = FeedMessageAggregator.startOfDay(message.posted);
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
                    newMessages.add(new AggregateFriendMessage(true, message.type,
                        message.posted, lvalue.getList()));
                    if (rsize > 1) {
                        rvalue.remove(message);
                    }
                    lvalue.setDisplayed(true);
                    continue;
                } else if (rsize > 1) {
                    newMessages.add(new AggregateFriendMessage(false, message.type,
                        message.posted, rvalue.getList()));
                    if (lsize > 1) {
                        lvalue.remove(message);
                    }
                    rvalue.setDisplayed(true);
                    continue;
                } else {
                    newMessages.add(message);
                }
            }
            messageMapLeft.clear();
            messageMapRight.clear();
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
     * Get the key for left side aggregation. Multiple actions by the same person (eg listing new
     * things in the shop).
     */
    public static MessageKey getLeftKey (FeedMessage message)
    {
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
        case 102: // FRIEND_WON_TROPHY
        case 103: // FRIEND_LISTED_ITEM
        case 105: // FRIEND_WON_BADGE
        case 101: // FRIEND_UPDATED_ROOM
            // group all these by the friend doing the actions
            return new MessageKey(message.type, ((FriendFeedMessage)message).friend.getMemberId());
        case 104: // FRIEND_GAINED_LEVEL
            // all level gains by all friends are displayed together
            return new MessageKey(message.type, 0);
        }
        return null;
    }

    /**
     * Get the key for right side aggregation. Multiple people performing the same action (eg
     * winning the same trophy).
     */
    public static MessageKey getRightKey (FeedMessage message)
    {
        switch (message.type) {
        case 100: // FRIEND_ADDED_FRIEND
            // one or more friends added a person to their friends, that person is the key
            return new MessageKey(message.type, message.data[1]);
        case 102: // FRIEND_WON_TROPHY
            // one or more friends earned the same trophy; trophy name and id is the key
            return new MessageKey(message.type, message.data[1].concat(message.data[0]).hashCode());
        case 105: // FRIEND_WON_BADGE
            // one or more friends earned the same badge; badge id and level is the key
            return new MessageKey(message.type, message.data[0].concat(message.data[1]).hashCode());
        }
        return null;
    }

    /**
     * Builds a left side or right side aggregated HashMap for the supplied messages.
     */
    public static void buildMessageMap (List<FeedMessage> messages, long header,
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
     * An aggregate message conatining multiple actions by the same actor (left=true), or actors
     * performing the same action (left=false). Duplicate items will be removed at this point.
     */
    public static class AggregateFriendMessage extends FeedMessage
    {
        public AggregateFriendMessage (boolean left, int type, long posted,
                List<FriendFeedMessage> messages)
        {
            this.left = left;
            this.type = type;
            this.posted = posted;
            this.messages = new ArrayList<FriendFeedMessage>();
            for (FriendFeedMessage message : messages) {
                if (!contains(message)) {
                    this.messages.add(message);
                }
            }
        }
        public List<FriendFeedMessage> messages;
        public boolean left;

        /**
         * Compare the given message with the contents of the list. Check for duplicates is based
         * on the type of message: don't show the same player updating the same room twice, or the
         * same player gaining more than one level.
         */
        protected boolean contains (FriendFeedMessage message)
        {
            switch(message.type) {
            case 101: // FRIEND_UPDATED_ROOM
                // don't show the same friend updating the same room id twice
                for (FriendFeedMessage msg : this.messages) {
                    if (msg.friend.equals(message.friend)
                        && msg.data[0].equals(message.data[0])) {
                        return true;
                    }
                }
                break;
            case 104: // FRIEND_GAINED_LEVEL
                // don't show the same friend's level gain more than once
                for (FriendFeedMessage msg : this.messages) {
                    if (msg.friend.equals(message.friend)) {
                        return true;
                    }
                }
                break;
            }
            return false;
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
                CShell.log("Ignoring addition of messages to a MessageAggregate that has been displayed");
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
}
