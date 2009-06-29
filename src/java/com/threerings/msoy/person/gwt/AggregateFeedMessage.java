//
// $Id$

package com.threerings.msoy.person.gwt;

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
        ACTORS;

        /**
         * Get the opposite style.
         */
        public Style getOpposite () {
            Style[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
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
        super(type, null, posted);
        this.style = style;
        this.messages = messages;
    }
}
