//
// $Id$

package com.threerings.msoy.person.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a feed message.
 */
public class FeedMessage
    implements IsSerializable
{
    /** The type of feed message. */
    public FeedMessageType type;

    /** The arguments to this feed message. */
    public String[] data;

    /** The time at which this message was posted. */
    public long posted;

    /**
     * Constructs a new feed message for serialization.
     */
    public FeedMessage ()
    {
    }

    /**
     * Constructs a new feed message with the given field values.
     */
    public FeedMessage (FeedMessageType type, String[] data, long posted)
    {
        this.type = type;
        this.data = data;
        this.posted = posted;
    }
}
