//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.ArrayList;
import java.util.List;

/**
 * An aggregate message containing multiple actions by the same actor, or multiple actors performing
 * the same action. Also prevents duplicate messages.
 */
public class AggregateFeedMessage extends FeedMessage
{
    /** Styles of aggregation. */
    public enum Style
    {
        /** Many actions, one actor. */
        ACTIONS,

        /** Many actors, one action. */
        ACTORS
    };

    /** The style of aggregation for this message. */
    public Style style;

    /** The messages we are aggregating. */
    public List<FeedMessage> messages;

    /**
     * Creates a new aggregate message.
     */
    public AggregateFeedMessage (
        Style style, FeedMessageType type, long posted, List<FeedMessage> messages)
    {
        this.style = style;
        this.type = type;
        this.posted = posted;
        this.messages = new ArrayList<FeedMessage>();
        for (FeedMessage message : messages) {
            if (!isDuplicate(message)) {
                this.messages.add(message);
            }
        }
    }

    /**
     * Compares the given message with the contents of the list. Check for duplicates is based
     * on the type of message: don't show the same player updating the same room twice, or the
     * same player gaining more than one level.
     */
    protected boolean isDuplicate (FeedMessage message)
    {
        switch (message.type) {
        case FRIEND_UPDATED_ROOM:
            // don't show the same friend updating the same room id twice
            for (FeedMessage msg : this.messages) {
                if (((FriendFeedMessage)msg).friend.equals(((FriendFeedMessage)message).friend)
                    && msg.data[0].equals(message.data[0])) {
                    return true;
                }
            }
            break;

        case FRIEND_GAINED_LEVEL:
            // don't show the same friend's level gain more than once
            for (FeedMessage msg : this.messages) {
                if (((FriendFeedMessage)msg).friend.equals(
                        ((FriendFeedMessage)message).friend)) {
                    return true;
                }
            }
            break;

        case SELF_ROOM_COMMENT:
        case SELF_ITEM_COMMENT:
            // don't show the same friend's comment more than once
            for (FeedMessage msg : this.messages) {
                if (((SelfFeedMessage)msg).actor.equals(((SelfFeedMessage)message).actor)) {
                    return true;
                }
            }
            break;
        }
        return false;
    }
}
