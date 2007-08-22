//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;

/**
 * A computed entity that receives the count(*) results for read and unread messages in a folder.
 */
@Computed(shadowOf=MailMessageRecord.class)
@Entity
public class MailCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #unread} field. */
    public static final String UNREAD = "unread";

    /** The qualified column identifier for the {@link #unread} field. */
    public static final ColumnExp UNREAD_C =
        new ColumnExp(MailCountRecord.class, UNREAD);

    /** The column identifier for the {@link #count} field. */
    public static final String COUNT = "count";

    /** The qualified column identifier for the {@link #count} field. */
    public static final ColumnExp COUNT_C =
        new ColumnExp(MailCountRecord.class, COUNT);
    // AUTO-GENERATED: FIELDS END

    /** Whether this count is an unread count or a read count. */
    public boolean unread;

    /** The number of messages that are unread (or read). */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
