//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

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
    /** The column identifier for the {@link #lastDistributedBling} field. */
    public static final String LAST_DISTRIBUTED_BLING = "lastDistributedBling";

    /** The qualified column identifier for the {@link #lastDistributedBling} field. */
    public static final ColumnExp LAST_DISTRIBUTED_BLING_C =
        new ColumnExp(MoneyConfigRecord.class, LAST_DISTRIBUTED_BLING);

    /** The column identifier for the {@link #id} field. */
    public static final String ID = "id";

    /** The qualified column identifier for the {@link #id} field. */
    public static final ColumnExp ID_C =
        new ColumnExp(MoneyConfigRecord.class, ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;
    
    /** The date/time bling was last distributed from the bling pool. Null indicates never run. **/
    public Timestamp lastDistributedBling;
    
    /** ID of the record, primarily just so we have a primary key. */
    @Id
    public int id;

    /**
     * Constructs a new money config record, setting the primary key to 1 and using the current
     * time for {@link #lastDistributedBling}.
     */
    public MoneyConfigRecord ()
    {
        this.id = 1;        // The record will always be ID = 1
        this.lastDistributedBling = new Timestamp(System.currentTimeMillis());
    }
    
    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MoneyConfigRecord}
     * with the supplied key values.
     */
    public static Key<MoneyConfigRecord> getKey (int id)
    {
        return new Key<MoneyConfigRecord>(
                MoneyConfigRecord.class,
                new String[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
