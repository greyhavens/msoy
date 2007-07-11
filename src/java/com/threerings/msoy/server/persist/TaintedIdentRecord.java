//
// $Id: TaintedIdent.java 1954 2006-06-15 22:47:48Z mdb $

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Represents a row in the TAINTED_IDENTS table, Depot version.
 */
@Entity(name="TAINTED_IDENTS")
public class TaintedIdentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #machIdent} field. */
    public static final String MACH_IDENT = "machIdent";

    /** The qualified column identifier for the {@link #machIdent} field. */
    public static final ColumnExp MACH_IDENT_C =
        new ColumnExp(TaintedIdentRecord.class, MACH_IDENT);
    // AUTO-GENERATED: FIELDS END

    /** A 'unique' id for a specific machine we have seen. */
    @Id @Column(name="MACH_IDENT")
    public String machIdent;

    /** Blank constructor for the unserialization buisness. */
    public TaintedIdentRecord ()
    {
    }

    /** A constructor that populates this record. */
    public TaintedIdentRecord (String machIdent)
    {
        this.machIdent = machIdent;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #TaintedIdentRecord}
     * with the supplied key values.
     */
    public static Key<TaintedIdentRecord> getKey (String machIdent)
    {
        return new Key<TaintedIdentRecord>(
                TaintedIdentRecord.class,
                new String[] { MACH_IDENT },
                new Comparable[] { machIdent });
    }
    // AUTO-GENERATED: METHODS END
}
