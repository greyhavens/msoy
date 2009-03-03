//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

import com.threerings.msoy.person.gwt.FeedMessage;

/**
 * Contains information on a feed message.
 */
@Entity
public abstract class FeedMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FeedMessageRecord> _R = FeedMessageRecord.class;
    public static final ColumnExp MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp DATA = colexp(_R, "data");
    public static final ColumnExp POSTED = colexp(_R, "posted");
    // AUTO-GENERATED: FIELDS END

    /** A unique identifier for this message. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int messageId;

    /** Used to determine how to format and display the feed message. */
    public int type;

    /** A tab separated list of strings that comprise this message's data. */
    public String data;

    /** The time at which this message was posted. */
    @Index(name="ixPosted")
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
