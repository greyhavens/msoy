//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.person.gwt.FeedMessage;

/**
 * Contains information on a feed message.
 */
public abstract class FeedMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #messageId} field. */
    public static final String MESSAGE_ID = "messageId";

    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The column identifier for the {@link #data} field. */
    public static final String DATA = "data";

    /** The column identifier for the {@link #posted} field. */
    public static final String POSTED = "posted";
    // AUTO-GENERATED: FIELDS END

    /** A unique identifier for this message. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int messageId;

    /** Used to determine how to format and display the feed message. */
    public int type;

    /** A tab separated list of strings that comprise this message's data. */
    public String data;

    /** The time at which this message was posted. */
    public Timestamp posted;

    /**
     * Converts this persistent record to a runtime record.
     */
    public FeedMessage toMessage ()
    {
        FeedMessage message = createMessage();
        message.type = type;
        message.data = data.split("\t");
        message.posted = posted.getTime();
        return message;
    }

    protected abstract FeedMessage createMessage ();
}
