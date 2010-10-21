//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.HashMediaDesc;

/**
 * A blacklisted bit of media, represented as hash/mimeType.
 */
@Entity
public class MediaBlacklistRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MediaBlacklistRecord> _R = MediaBlacklistRecord.class;
    public static final ColumnExp MEDIA_HASH = colexp(_R, "mediaHash");
    public static final ColumnExp MIME_TYPE = colexp(_R, "mimeType");
    public static final ColumnExp TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp NOTE = colexp(_R, "note");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The hashcode of this blacklisted media. */
    @Index
    public byte[] mediaHash;

    /** The MIME type of the {@link #mediaHash} media. */
    public byte mimeType;

    /** When this media was blacklisted. */
    public Timestamp timestamp;

    /** A human-readable summary of the reason behind the blacklisting. */
    public String note;

    public MediaBlacklistRecord ()
    {
    }

    public MediaBlacklistRecord (HashMediaDesc desc, String note)
    {
        this.mediaHash = desc.hash;
        this.mimeType = desc.getMimeType();

        this.timestamp = new Timestamp(System.currentTimeMillis());

        this.note = note;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

}
