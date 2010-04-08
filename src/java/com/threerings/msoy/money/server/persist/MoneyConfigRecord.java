//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Date;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.Calendars;

/**
 * Contains current run-time information about the money service.  There will only ever be a single
 * record in this table.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity
@NotThreadSafe
public class MoneyConfigRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MoneyConfigRecord> _R = MoneyConfigRecord.class;
    public static final ColumnExp LAST_DISTRIBUTED_BLING = colexp(_R, "lastDistributedBling");
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp LOCKED = colexp(_R, "locked");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The ID of the record, always. */
    public static final int RECORD_ID = 1;

    /** The date bling was last distributed from the bling pool. Null indicates never run. **/
    public Date lastDistributedBling;

    /** ID of the record, primarily just so we have a primary key. */
    @Id
    public int id;

    /** If true, indicates this record is locked for performing some operation. */
    public boolean locked;

    /**
     * Constructs a new money config record, setting the primary key to 1 and using the current
     * time for {@link #lastDistributedBling}.
     */
    public MoneyConfigRecord ()
    {
        this.id = RECORD_ID;        // The record will always be ID = 1

        // Use the previous day, so we can calculate today's winnings later.
        this.lastDistributedBling = Calendars.now().addDays(-1).toSQLDate();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MoneyConfigRecord}
     * with the supplied key values.
     */
    public static Key<MoneyConfigRecord> getKey (int id)
    {
        return newKey(_R, id);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ID); }
    // AUTO-GENERATED: METHODS END
}
