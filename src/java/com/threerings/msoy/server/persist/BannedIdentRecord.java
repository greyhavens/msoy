//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Represents a row in the BANNED_IDENTS table, Depot version.
 */
@Entity(name="BANNED_IDENTS")
public class BannedIdentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #machIdent} field. */
    public static final String MACH_IDENT = "machIdent";

    /** The qualified column identifier for the {@link #machIdent} field. */
    public static final ColumnExp MACH_IDENT_C =
        new ColumnExp(BannedIdentRecord.class, MACH_IDENT);

    /** The column identifier for the {@link #siteId} field. */
    public static final String SITE_ID = "siteId";

    /** The qualified column identifier for the {@link #siteId} field. */
    public static final ColumnExp SITE_ID_C =
        new ColumnExp(BannedIdentRecord.class, SITE_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** a 'unique' id for the specific machine we have seen. */
    @Id @Column(name="MACH_IDENT")
    public String machIdent;

    /** The site if which this machine is banned from. */
    @Id @Column(name="SITE_ID")
    public int siteId;

    /** Blank constructor for the unserialization business. */
    public BannedIdentRecord ()
    {
    }

    /** A constructor that populates this record. */
    public BannedIdentRecord (String machIdent, int siteId)
    {
        this.machIdent = machIdent;
        this.siteId = siteId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #BannedIdentRecord}
     * with the supplied key values.
     */
    public static Key<BannedIdentRecord> getKey (String machIdent, int siteId)
    {
        return new Key<BannedIdentRecord>(
                BannedIdentRecord.class,
                new String[] { MACH_IDENT, SITE_ID },
                new Comparable[] { machIdent, siteId });
    }
    // AUTO-GENERATED: METHODS END
}
