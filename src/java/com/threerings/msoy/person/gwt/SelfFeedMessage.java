//
// $Id$

package com.threerings.msoy.person.gwt;

import com.threerings.msoy.data.all.MemberName;

/**
 * Typed extension of FeedMessage for SelfFeedMessages.  On the client, these messages are
 * presented in the context of the member whose profile they are shown on.
 */
public class SelfFeedMessage extends FeedMessage
{
    /** The person that triggered this feed message, or null. */
    public MemberName actor;

    /**
     * Constructs a new self feed message for serialization.
     */
    public SelfFeedMessage ()
    {
    }

    /**
     * Constructs a new self feed message with the given field values.
     */
    public SelfFeedMessage (FeedMessageType type, MemberName actor, String[] data, long posted)
    {
        super(type, data, posted);
        this.actor = actor;
    }
}
